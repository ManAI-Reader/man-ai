package com.highliuk.manai.domain.usecase

import com.highliuk.manai.domain.model.Manga
import com.highliuk.manai.domain.repository.MangaRepository
import javax.inject.Inject

class ImportMangaUseCase @Inject constructor(
    private val repository: MangaRepository,
) {
    suspend operator fun invoke(title: String, filePath: String, pageCount: Int): Long {
        val manga = Manga(title = title, filePath = filePath, pageCount = pageCount)
        return repository.insertManga(manga)
    }
}
