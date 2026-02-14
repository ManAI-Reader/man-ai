package com.highliuk.manai.domain.repository

import com.highliuk.manai.domain.model.Manga
import kotlinx.coroutines.flow.Flow

interface MangaRepository {
    fun getAllManga(): Flow<List<Manga>>
    suspend fun insertManga(manga: Manga)
}
