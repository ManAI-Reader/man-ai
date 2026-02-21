package com.highliuk.manai.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.highliuk.manai.data.hash.FileHashProvider
import com.highliuk.manai.data.pdf.PdfFileManager
import com.highliuk.manai.data.pdf.PdfMetadataExtractor
import com.highliuk.manai.domain.model.Manga
import com.highliuk.manai.domain.repository.MangaRepository
import com.highliuk.manai.domain.repository.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: MangaRepository,
    private val pdfMetadataExtractor: PdfMetadataExtractor,
    private val fileHashProvider: FileHashProvider,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val pdfFileCopier: PdfFileManager
) : ViewModel() {

    val mangaList: StateFlow<List<Manga>> = repository.getAllManga()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val gridColumns: StateFlow<Int> = userPreferencesRepository.gridColumns
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 2)

    private val _selectedMangaIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedMangaIds: StateFlow<Set<Long>> = _selectedMangaIds.asStateFlow()
    val isSelectionMode: StateFlow<Boolean> = _selectedMangaIds
        .map { it.isNotEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun toggleSelection(id: Long) {
        _selectedMangaIds.update { current ->
            if (id in current) current - id else current + id
        }
    }

    fun clearSelection() {
        _selectedMangaIds.update { emptySet() }
    }

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

    private val _showDeleteDialog = MutableStateFlow(false)
    val showDeleteDialog: StateFlow<Boolean> = _showDeleteDialog.asStateFlow()

    fun requestDelete() { _showDeleteDialog.value = true }
    fun dismissDelete() { _showDeleteDialog.value = false }
    fun confirmDelete() {
        deleteSelectedManga()
        _showDeleteDialog.value = false
    }

    fun deleteSelectedManga() {
        viewModelScope.launch {
            val ids = _selectedMangaIds.value.toList()
            val mangasToDelete = mangaList.value.filter { it.id in ids }
            mangasToDelete.forEach { pdfFileCopier.deleteLocalCopy(it.uri) }
            repository.deleteMangaByIds(ids)
            clearSelection()
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
