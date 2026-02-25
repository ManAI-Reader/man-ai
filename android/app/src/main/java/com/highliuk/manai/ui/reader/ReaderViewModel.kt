package com.highliuk.manai.ui.reader

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.highliuk.manai.data.pdf.PdfPageRenderer
import com.highliuk.manai.domain.model.Manga
import com.highliuk.manai.domain.model.PageRegion
import com.highliuk.manai.domain.model.ReadingMode
import com.highliuk.manai.domain.repository.MangaRepository
import com.highliuk.manai.domain.repository.OcrCacheRepository
import com.highliuk.manai.domain.repository.UserPreferencesRepository
import com.highliuk.manai.domain.usecase.ProcessPageUseCase
import com.highliuk.manai.domain.usecase.WarmUpOnnxUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

@Suppress("LongParameterList")
@OptIn(FlowPreview::class, kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@HiltViewModel
class ReaderViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: MangaRepository,
    userPreferencesRepository: UserPreferencesRepository,
    private val processPageUseCase: ProcessPageUseCase,
    private val warmUpOnnxUseCase: WarmUpOnnxUseCase,
    private val ocrCache: OcrCacheRepository,
    private val pdfPageRenderer: PdfPageRenderer,
) : ViewModel() {

    private val mangaId: Long = savedStateHandle["mangaId"] ?: 0L

    val readingMode: StateFlow<ReadingMode> = userPreferencesRepository.readingMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ReadingMode.LTR)

    val manga: StateFlow<Manga?> = repository.getMangaById(mangaId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _currentPage = MutableStateFlow(0)
    val currentPage: StateFlow<Int> = _currentPage.asStateFlow()

    val currentPageRegions: StateFlow<List<PageRegion>> = _currentPage
        .flatMapLatest { page -> ocrCache.observeRegions(mangaId, page) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedRegion = MutableStateFlow<PageRegion?>(null)
    val selectedRegion: StateFlow<PageRegion?> = _selectedRegion.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private var pipelineJob: Job? = null

    init {
        viewModelScope.launch {
            warmUpOnnxUseCase.execute()
        }

        viewModelScope.launch {
            val loadedManga = manga.filterNotNull().first()
            _currentPage.value = loadedManga.lastReadPage
        }

        viewModelScope.launch {
            _currentPage
                .drop(1)
                .debounce(500L)
                .collect { page ->
                    repository.updateLastReadPage(mangaId, page)
                }
        }

        viewModelScope.launch {
            _currentPage
                .debounce(300L)
                .collect { page ->
                    launchPipeline(page)
                }
        }
    }

    fun onPageChanged(page: Int) {
        _currentPage.value = page
    }

    fun onRegionTapped(region: PageRegion) {
        _selectedRegion.value = region
        if (region.ocrText == null) {
            launchPipeline(_currentPage.value, priorityRegionIndex = region.regionIndex)
        }
    }

    fun dismissBottomSheet() {
        _selectedRegion.value = null
    }

    internal fun launchPipeline(pageIndex: Int, priorityRegionIndex: Int? = null) {
        pipelineJob?.cancel()
        pipelineJob = viewModelScope.launch {
            val uri = manga.value?.uri ?: return@launch
            _isProcessing.value = true
            try {
                val bitmap = pdfPageRenderer.render(uri, pageIndex) ?: return@launch
                processPageUseCase.execute(mangaId, pageIndex, bitmap,
                    priorityRegionIndex = priorityRegionIndex)

                val nextPage = pageIndex + 1
                val pageCount = manga.value?.pageCount ?: 0
                if (nextPage < pageCount) {
                    val nextBitmap = pdfPageRenderer.render(uri, nextPage)
                    if (nextBitmap != null) {
                        processPageUseCase.execute(mangaId, nextPage, nextBitmap,
                            detectionOnly = true)
                    }
                }
            } finally {
                _isProcessing.value = false
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch {
            repository.updateLastReadPage(mangaId, _currentPage.value)
        }
    }
}
