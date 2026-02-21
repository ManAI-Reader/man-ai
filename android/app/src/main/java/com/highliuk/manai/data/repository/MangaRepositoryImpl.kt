package com.highliuk.manai.data.repository

import com.highliuk.manai.data.local.dao.MangaDao
import com.highliuk.manai.data.local.entity.MangaEntity
import com.highliuk.manai.domain.model.Manga
import com.highliuk.manai.domain.repository.MangaRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class MangaRepositoryImpl @Inject constructor(
    private val mangaDao: MangaDao
) : MangaRepository {

    override fun getAllManga(): Flow<List<Manga>> =
        mangaDao.getAll().map { entities ->
            entities.map { it.toManga() }
        }

    override fun getMangaById(id: Long): Flow<Manga?> =
        mangaDao.getById(id).map { it?.toManga() }

    override suspend fun insertManga(manga: Manga): Long =
        mangaDao.insert(MangaEntity.fromManga(manga))

    override suspend fun upsertManga(manga: Manga): Long =
        mangaDao.upsertByContentHash(MangaEntity.fromManga(manga))

    override suspend fun getMangaByUri(uri: String): Manga? =
        mangaDao.getByUri(uri)?.toManga()

    override suspend fun getMangaByContentHash(contentHash: String): Manga? =
        mangaDao.getByContentHash(contentHash)?.toManga()

    override suspend fun updateLastReadPage(id: Long, page: Int) {
        mangaDao.updateLastReadPage(id, page)
    }

    override suspend fun deleteMangaByIds(ids: List<Long>) {
        mangaDao.deleteByIds(ids)
    }
}
