package com.highliuk.manai.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.highliuk.manai.data.hash.FileHashProvider
import com.highliuk.manai.data.pdf.PdfMetadataExtractor
import com.highliuk.manai.domain.model.Manga
import com.highliuk.manai.domain.repository.MangaRepository
import com.highliuk.manai.domain.repository.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: MangaRepository,
    private val pdfMetadataExtractor: PdfMetadataExtractor,
    private val fileHashProvider: FileHashProvider,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    val mangaList: StateFlow<List<Manga>> = repository.getAllManga()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val gridColumns: StateFlow<Int> = userPreferencesRepository.gridColumns
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 2)

    private val _navigateToReader = MutableSharedFlow<Long>()
    val navigateToReader: SharedFlow<Long> = _navigateToReader.asSharedFlow()

    fun importManga(uri: String, fileName: String) {
        viewModelScope.launch {
            try {
                val title = fileName.removeSuffix(".pdf")
                val pageCount = pdfMetadataExtractor.extractPageCount(uri)
                val contentHash = fileHashProvider.computeHash(uri)
                val id = repository.upsertManga(
                    Manga(uri = uri, title = title, pageCount = pageCount, contentHash = contentHash)
                )
                _navigateToReader.emit(id)
            } catch (_: Exception) {
                // PDF could not be opened or read — silently ignore
            }
        }
    }
}
