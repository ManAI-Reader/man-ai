package com.highliuk.manai.data.repository

import com.highliuk.manai.data.local.dao.MangaDao
import com.highliuk.manai.data.local.entity.MangaEntity
import com.highliuk.manai.domain.model.Manga
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MangaRepositoryImplTest {

    private val dao = mockk<MangaDao>(relaxed = true)
    private val repository = MangaRepositoryImpl(dao)

    @Test
    fun `getAllManga maps entities to domain models`() = runTest {
        val entities = listOf(
            MangaEntity(id = 1, uri = "uri1", title = "Manga 1", pageCount = 10),
            MangaEntity(id = 2, uri = "uri2", title = "Manga 2", pageCount = 20)
        )
        coEvery { dao.getAll() } returns flowOf(entities)

        val result = repository.getAllManga().first()

        assertEquals(2, result.size)
        assertEquals("Manga 1", result[0].title)
        assertEquals(10, result[0].pageCount)
        assertEquals("Manga 2", result[1].title)
    }

    @Test
    fun `getMangaById maps entity to domain model`() = runTest {
        val entity = MangaEntity(id = 1, uri = "uri1", title = "Test", pageCount = 10)
        coEvery { dao.getById(1) } returns flowOf(entity)

        val result = repository.getMangaById(1).first()

        assertEquals("Test", result?.title)
        assertEquals(10, result?.pageCount)
    }

    @Test
    fun `getMangaById returns null when not found`() = runTest {
        coEvery { dao.getById(999) } returns flowOf(null)

        val result = repository.getMangaById(999).first()

        assertNull(result)
    }

    @Test
    fun `insertManga converts domain model to entity and delegates to dao`() = runTest {
        val manga = Manga(uri = "uri1", title = "Test", pageCount = 5)

        repository.insertManga(manga)

        coVerify {
            dao.insert(match {
                it.uri == "uri1" && it.title == "Test" && it.pageCount == 5
            })
        }
    }

    @Test
    fun `upsertManga delegates to dao upsertByContentHash`() = runTest {
        val manga = Manga(uri = "uri1", title = "Test", pageCount = 5, contentHash = "hash123")
        coEvery { dao.upsertByContentHash(any()) } returns 42L

        val result = repository.upsertManga(manga)

        assertEquals(42L, result)
        coVerify {
            dao.upsertByContentHash(match {
                it.uri == "uri1" && it.contentHash == "hash123"
            })
        }
    }

    @Test
    fun `getMangaByContentHash returns domain model`() = runTest {
        val entity = MangaEntity(
            id = 1, uri = "uri1", title = "Test", pageCount = 10, contentHash = "hash123"
        )
        coEvery { dao.getByContentHash("hash123") } returns entity

        val result = repository.getMangaByContentHash("hash123")

        assertEquals("Test", result?.title)
        assertEquals("hash123", result?.contentHash)
    }

    @Test
    fun `getMangaByContentHash returns null when not found`() = runTest {
        coEvery { dao.getByContentHash("nonexistent") } returns null

        val result = repository.getMangaByContentHash("nonexistent")

        assertNull(result)
    }

    @Test
    fun `updateLastReadPage delegates to dao`() = runTest {
        repository.updateLastReadPage(1L, 42)

        coVerify { dao.updateLastReadPage(1L, 42) }
    }
}
