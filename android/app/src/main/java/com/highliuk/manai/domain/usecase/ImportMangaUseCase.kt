package com.highliuk.manai.domain.usecase

import com.highliuk.manai.domain.model.Manga
import com.highliuk.manai.domain.repository.MangaRepository
import javax.inject.Inject

class ImportMangaUseCase @Inject constructor(
    private val repository: MangaRepository,
) {
    suspend operator fun invoke(title: String, filePath: String, pageCount: Int): Long {
        val existing = repository.getMangaByFilePath(filePath)
        if (existing != null) return existing.id

        val manga = Manga(title = title, filePath = filePath, pageCount = pageCount)
        return repository.insertManga(manga)
    }
}
