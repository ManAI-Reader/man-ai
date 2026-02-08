package com.highliuk.manai.data.repository

import com.highliuk.manai.data.local.dao.MangaDao
import com.highliuk.manai.data.local.entity.MangaEntity
import com.highliuk.manai.domain.model.Manga
import com.highliuk.manai.domain.repository.MangaRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class MangaRepositoryImpl @Inject constructor(
    private val mangaDao: MangaDao,
) : MangaRepository {

    override fun getMangaList(): Flow<List<Manga>> =
        mangaDao.getAll().map { entities -> entities.map { it.toDomain() } }

    override suspend fun getMangaById(id: Long): Manga? =
        mangaDao.getById(id)?.toDomain()

    override suspend fun getMangaByFilePath(filePath: String): Manga? =
        mangaDao.getByFilePath(filePath)?.toDomain()

    override suspend fun insertManga(manga: Manga): Long =
        mangaDao.insert(manga.toEntity())

    override suspend fun updateLastReadPage(id: Long, page: Int) =
        mangaDao.updateLastReadPage(id, page)

    override suspend fun deleteManga(id: Long) =
        mangaDao.delete(id)

    private fun MangaEntity.toDomain() = Manga(
        id = id,
        title = title,
        filePath = filePath,
        pageCount = pageCount,
        lastReadPage = lastReadPage,
        addedAt = addedAt,
    )

    private fun Manga.toEntity() = MangaEntity(
        id = id,
        title = title,
        filePath = filePath,
        pageCount = pageCount,
        lastReadPage = lastReadPage,
        addedAt = addedAt,
    )
}
