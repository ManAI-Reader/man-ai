package com.highliuk.manai.domain.usecase

import com.highliuk.manai.domain.model.Manga
import com.highliuk.manai.domain.repository.MangaRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class GetMangaListUseCaseTest {

    private val repository = mockk<MangaRepository>()
    private val useCase = GetMangaListUseCase(repository)

    @Test
    fun `invoke returns manga list from repository`() = runTest {
        val expected = listOf(
            Manga(id = 1, title = "Manga 1", filePath = "/path/1.pdf", pageCount = 100),
            Manga(id = 2, title = "Manga 2", filePath = "/path/2.pdf", pageCount = 200),
        )
        every { repository.getMangaList() } returns flowOf(expected)

        val result = useCase().first()

        assertEquals(expected, result)
    }

    @Test
    fun `invoke returns empty list when no manga`() = runTest {
        every { repository.getMangaList() } returns flowOf(emptyList())

        val result = useCase().first()

        assertEquals(emptyList<Manga>(), result)
    }
}
