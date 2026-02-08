package com.highliuk.manai.data.repository

import com.highliuk.manai.data.local.dao.MangaDao
import com.highliuk.manai.data.local.entity.MangaEntity
import com.highliuk.manai.domain.model.Manga
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MangaRepositoryImplTest {

    private val mangaDao = mockk<MangaDao>(relaxed = true)
    private val repository = MangaRepositoryImpl(mangaDao)

    @Test
    fun `getMangaList maps entities to domain models`() = runTest {
        val entities = listOf(
            MangaEntity(
                id = 1,
                title = "Manga 1",
                filePath = "/path/1.pdf",
                pageCount = 100,
                addedAt = 1000L,
            ),
        )
        every { mangaDao.getAll() } returns flowOf(entities)

        val result = repository.getMangaList().first()

        assertEquals(1, result.size)
        assertEquals("Manga 1", result[0].title)
        assertEquals("/path/1.pdf", result[0].filePath)
    }

    @Test
    fun `getMangaById returns null when not found`() = runTest {
        coEvery { mangaDao.getById(99) } returns null

        val result = repository.getMangaById(99)

        assertNull(result)
    }

    @Test
    fun `getMangaById returns domain model when found`() = runTest {
        val entity = MangaEntity(
            id = 1,
            title = "Test",
            filePath = "/test.pdf",
            pageCount = 50,
            addedAt = 1000L,
        )
        coEvery { mangaDao.getById(1) } returns entity

        val result = repository.getMangaById(1)

        assertEquals("Test", result?.title)
    }

    @Test
    fun `insertManga delegates to dao`() = runTest {
        coEvery { mangaDao.insert(any()) } returns 1L

        val manga = Manga(title = "New", filePath = "/new.pdf", pageCount = 30)
        val id = repository.insertManga(manga)

        assertEquals(1L, id)
        coVerify { mangaDao.insert(any()) }
    }

    @Test
    fun `deleteManga delegates to dao`() = runTest {
        repository.deleteManga(1)

        coVerify { mangaDao.delete(1) }
    }

    @Test
    fun `getMangaByFilePath returns domain model when found`() = runTest {
        val entity = MangaEntity(
            id = 3,
            title = "Found",
            filePath = "content://test/doc.pdf",
            pageCount = 20,
            addedAt = 2000L,
        )
        coEvery { mangaDao.getByFilePath("content://test/doc.pdf") } returns entity

        val result = repository.getMangaByFilePath("content://test/doc.pdf")

        assertEquals(3L, result?.id)
        assertEquals("Found", result?.title)
    }

    @Test
    fun `getMangaByFilePath returns null when not found`() = runTest {
        coEvery { mangaDao.getByFilePath("content://unknown") } returns null

        val result = repository.getMangaByFilePath("content://unknown")

        assertNull(result)
    }
}
