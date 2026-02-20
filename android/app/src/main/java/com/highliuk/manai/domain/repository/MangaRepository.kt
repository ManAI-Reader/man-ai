package com.highliuk.manai.domain.repository

import com.highliuk.manai.domain.model.Manga
import kotlinx.coroutines.flow.Flow

interface MangaRepository {
    fun getAllManga(): Flow<List<Manga>>
    fun getMangaById(id: Long): Flow<Manga?>
    suspend fun insertManga(manga: Manga): Long
    suspend fun upsertManga(manga: Manga): Long
    suspend fun getMangaByUri(uri: String): Manga?
    suspend fun getMangaByContentHash(contentHash: String): Manga?
    suspend fun updateLastReadPage(id: Long, page: Int)
}
