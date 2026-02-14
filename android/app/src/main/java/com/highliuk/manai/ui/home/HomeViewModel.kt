package com.highliuk.manai.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.highliuk.manai.data.pdf.PdfMetadataExtractor
import com.highliuk.manai.domain.model.Manga
import com.highliuk.manai.domain.repository.MangaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: MangaRepository,
    private val pdfMetadataExtractor: PdfMetadataExtractor
) : ViewModel() {

    val mangaList: StateFlow<List<Manga>> = repository.getAllManga()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun importManga(uri: String, fileName: String) {
        viewModelScope.launch {
            try {
                val title = fileName.removeSuffix(".pdf")
                val pageCount = pdfMetadataExtractor.extractPageCount(uri)
                repository.insertManga(Manga(uri = uri, title = title, pageCount = pageCount))
            } catch (_: Exception) {
                // PDF could not be opened or read — silently ignore
            }
        }
    }
}
