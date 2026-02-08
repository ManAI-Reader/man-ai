package com.highliuk.manai.domain.usecase

import com.highliuk.manai.domain.model.Manga
import com.highliuk.manai.domain.repository.MangaRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ImportMangaUseCaseTest {

    private val repository = mockk<MangaRepository>(relaxed = true)
    private val useCase = ImportMangaUseCase(repository)

    @Test
    fun `invoke inserts manga when filePath is new`() = runTest {
        coEvery { repository.getMangaByFilePath("/new.pdf") } returns null
        coEvery { repository.insertManga(any()) } returns 1L

        val result = useCase("New Manga", "/new.pdf", 30)

        assertEquals(1L, result)
        coVerify { repository.insertManga(any()) }
    }

    @Test
    fun `invoke returns existing id without inserting when filePath already exists`() = runTest {
        val existing = Manga(id = 5, title = "Existing", filePath = "/dup.pdf", pageCount = 10)
        coEvery { repository.getMangaByFilePath("/dup.pdf") } returns existing

        val result = useCase("Existing", "/dup.pdf", 10)

        assertEquals(5L, result)
        coVerify(exactly = 0) { repository.insertManga(any()) }
    }
}
