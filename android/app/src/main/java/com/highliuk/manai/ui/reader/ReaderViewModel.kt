package com.highliuk.manai.ui.reader

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.highliuk.manai.domain.model.Manga
import com.highliuk.manai.domain.model.ReadingMode
import com.highliuk.manai.domain.repository.MangaRepository
import com.highliuk.manai.domain.repository.UserPreferencesRepository
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
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(FlowPreview::class)
@HiltViewModel
class ReaderViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: MangaRepository,
    userPreferencesRepository: UserPreferencesRepository,
) : ViewModel() {

    private val mangaId: Long = savedStateHandle["mangaId"] ?: 0L

    val readingMode: StateFlow<ReadingMode> = userPreferencesRepository.readingMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ReadingMode.LTR)

    val manga: StateFlow<Manga?> = repository.getMangaById(mangaId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _currentPage = MutableStateFlow(0)
    val currentPage: StateFlow<Int> = _currentPage.asStateFlow()

    init {
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
    }

    fun onPageChanged(page: Int) {
        _currentPage.value = page
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch {
            repository.updateLastReadPage(mangaId, _currentPage.value)
        }
    }
}
