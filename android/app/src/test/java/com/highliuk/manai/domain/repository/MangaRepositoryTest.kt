package com.highliuk.manai.domain.repository

import com.highliuk.manai.domain.model.Manga
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class MangaRepositoryTest {

    private val repository = mockk<MangaRepository>()

    @Test
    fun `getAllManga returns flow of manga list`() = runTest {
        val mangas = listOf(
            Manga(id = 1, uri = "uri1", title = "Manga 1", pageCount = 10)
        )
        coEvery { repository.getAllManga() } returns flowOf(mangas)

        val result = repository.getAllManga().first()
        assertEquals(mangas, result)
    }

    @Test
    fun `insertManga returns the inserted manga id`() = runTest {
        val manga = Manga(uri = "uri1", title = "Manga 1", pageCount = 10)
        coEvery { repository.insertManga(manga) } returns 1L

        val id = repository.insertManga(manga)
        assertEquals(1L, id)
    }
}
