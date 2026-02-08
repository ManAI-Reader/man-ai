package com.highliuk.manai.domain.repository

import com.highliuk.manai.domain.model.Manga
import kotlinx.coroutines.flow.Flow

interface MangaRepository {
    fun getMangaList(): Flow<List<Manga>>
    suspend fun getMangaById(id: Long): Manga?
    suspend fun insertManga(manga: Manga): Long
    suspend fun updateLastReadPage(id: Long, page: Int)
    suspend fun deleteManga(id: Long)
}
