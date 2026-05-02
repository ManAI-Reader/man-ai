package com.highliuk.manai.ui.reader

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.highliuk.manai.data.pdf.PdfPageRenderer
import com.highliuk.manai.domain.debug.DebugMlEvent
import com.highliuk.manai.domain.debug.DebugMlEventHolder
import com.highliuk.manai.domain.debug.PipelineDebugStateHolder
import com.highliuk.manai.domain.model.Manga
import com.highliuk.manai.domain.model.PagePipelineState
import com.highliuk.manai.domain.model.PageRegion
import com.highliuk.manai.domain.model.ReadingMode
import com.highliuk.manai.domain.model.TranslationResult
import com.highliuk.manai.domain.repository.MangaRepository
import com.highliuk.manai.domain.repository.OcrCacheRepository
import com.highliuk.manai.domain.repository.TranslationRepository
import com.highliuk.manai.domain.repository.UserPreferencesRepository
import com.highliuk.manai.domain.usecase.ProcessPageUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@Suppress("LongParameterList")
@OptIn(FlowPreview::class, kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@HiltViewModel
class ReaderViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: MangaRepository,
    userPreferencesRepository: UserPreferencesRepository,
    private val processPageUseCase: ProcessPageUseCase,
    private val ocrCache: OcrCacheRepository,
    private val pdfPageRenderer: PdfPageRenderer,
    debugStateHolder: PipelineDebugStateHolder,
    debugEventHolder: DebugMlEventHolder,
    private val translationRepository: TranslationRepository,
) : ViewModel() {

    val debugPipelineStates: StateFlow<Map<Int, PagePipelineState>> = debugStateHolder.states
    val debugEvents: Flow<DebugMlEvent> = debugEventHolder.events

    private val mangaId: Long = savedStateHandle["mangaId"] ?: 0L

    val readingMode: StateFlow<ReadingMode> = userPreferencesRepository.readingMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ReadingMode.LTR)

    val ocrFontScale: StateFlow<Float> = userPreferencesRepository.ocrFontScale
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1.5f)

    val tapToNavigatePortrait: StateFlow<Boolean> = userPreferencesRepository.tapToNavigatePortrait
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val tapToNavigateLandscape: StateFlow<Boolean> = userPreferencesRepository.tapToNavigateLandscape
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val manga: StateFlow<Manga?> = repository.getMangaById(mangaId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _currentPage = MutableStateFlow(0)
    val currentPage: StateFlow<Int> = _currentPage.asStateFlow()

    val currentPageRegions: StateFlow<List<PageRegion>> = _currentPage
        .flatMapLatest { page -> ocrCache.observeRegions(mangaId, page) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedRegion = MutableStateFlow<PageRegion?>(null)
    val selectedRegion: StateFlow<PageRegion?> = _selectedRegion.asStateFlow()

    sealed interface TranslationState {
        data object Idle : TranslationState
        data object Loading : TranslationState
        data class Translated(val text: String) : TranslationState
        data class Error(val message: String) : TranslationState
    }

    private val _translationState = MutableStateFlow<TranslationState>(TranslationState.Idle)
    val translationState: StateFlow<TranslationState> = _translationState.asStateFlow()

    private val _visiblePages = MutableStateFlow<List<Int>>(emptyList())

    val visiblePagesRegions: StateFlow<Map<Int, List<PageRegion>>> = _visiblePages
        .flatMapLatest { pages ->
            if (pages.isEmpty()) {
                kotlinx.coroutines.flow.flowOf(emptyMap())
            } else {
                combine(pages.map { page ->
                    ocrCache.observeRegions(mangaId, page).map { regions -> page to regions }
                }) { pairs ->
                    pairs.toMap()
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val pipelineJobs = mutableMapOf<Int, Job>()

    init {
        viewModelScope.launch {
            val loadedManga = manga.filterNotNull().first()
            _currentPage.value = loadedManga.lastReadPage
            launchPipeline(loadedManga.lastReadPage)
        }

        viewModelScope.launch {
            _currentPage
                .drop(1)
                .debounce(500L)
                .collect { page ->
                    repository.updateLastReadPage(mangaId, page)
                }
        }
    }

    fun onPageChanged(page: Int) {
        _currentPage.value = page
        launchPipeline(page)
    }

    fun onVisiblePagesChanged(visiblePages: List<Int>) {
        _visiblePages.value = visiblePages
        for (page in visiblePages) {
            launchPipeline(page)
        }
    }

    fun onRegionTapped(region: PageRegion) {
        _selectedRegion.value = region
        _translationState.value = TranslationState.Idle
        if (region.ocrText == null) {
            launchPipeline(region.pageIndex, priorityRegionIndex = region.regionIndex)
        } else {
            viewModelScope.launch {
                val cached = translationRepository.getCachedTranslation(
                    mangaId, region.pageIndex, region.regionIndex, region.ocrText
                )
                if (cached != null) {
                    _translationState.value = TranslationState.Translated(cached)
                }
            }
        }
    }

    fun dismissBottomSheet() {
        _selectedRegion.value = null
        _translationState.value = TranslationState.Idle
    }

    fun translateSelectedRegion() {
        val region = _selectedRegion.value ?: return
        val text = region.ocrText ?: return
        _translationState.value = TranslationState.Loading
        viewModelScope.launch {
            val result = translationRepository.translate(
                mangaId, region.pageIndex, region.regionIndex, text
            )
            _translationState.value = when (result) {
                is TranslationResult.Success -> TranslationState.Translated(result.text)
                is TranslationResult.Error -> TranslationState.Error(result.message)
            }
        }
    }

    internal fun launchPipeline(pageIndex: Int, priorityRegionIndex: Int? = null) {
        if (priorityRegionIndex == null && pipelineJobs[pageIndex]?.isActive == true) return
        pipelineJobs[pageIndex]?.cancel()
        pipelineJobs[pageIndex] = viewModelScope.launch {
            val uri = manga.value?.uri ?: return@launch
            _isProcessing.value = true
            try {
                val bitmap = pdfPageRenderer.render(uri, pageIndex) ?: return@launch
                processPageUseCase.execute(mangaId, pageIndex, bitmap,
                    priorityRegionIndex = priorityRegionIndex)
            } finally {
                pipelineJobs.remove(pageIndex)
                _isProcessing.value = pipelineJobs.any { it.value.isActive }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        runBlocking {
            repository.updateLastReadPage(mangaId, _currentPage.value)
        }
    }
}
