package com.highliuk.manai.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.highliuk.manai.data.hash.FileHashProvider
import com.highliuk.manai.data.pdf.PdfFileCopier
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
    private val userPreferencesRepository: UserPreferencesRepository,
    private val pdfFileCopier: PdfFileCopier
) : ViewModel() {

    val mangaList: StateFlow<List<Manga>> = repository.getAllManga()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val gridColumns: StateFlow<Int> = userPreferencesRepository.gridColumns
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 2)

    private val _navigateToReader = MutableSharedFlow<Long>()
    val navigateToReader: SharedFlow<Long> = _navigateToReader.asSharedFlow()

    private suspend fun performImport(uri: String, fileName: String): Long {
        val title = fileName.removeSuffix(".pdf")
        val pageCount = pdfMetadataExtractor.extractPageCount(uri)
        val contentHash = fileHashProvider.computeHash(uri)
        return repository.upsertManga(
            Manga(uri = uri, title = title, pageCount = pageCount, contentHash = contentHash)
        )
    }

    fun importManga(uri: String, fileName: String) {
        viewModelScope.launch {
            try {
                val id = performImport(uri, fileName)
                _navigateToReader.emit(id)
            } catch (_: Exception) {
                // PDF could not be opened or read — silently ignore
            }
        }
    }

    fun importMangaFromIntent(uri: String, fileName: String) {
        viewModelScope.launch {
            try {
                val localUri = pdfFileCopier.copyToLocalStorage(uri)
                val id = performImport(localUri, fileName)
                _navigateToReader.emit(id)
            } catch (_: Exception) {
                // Intent PDF could not be copied or read — silently ignore
            }
        }
    }
}
