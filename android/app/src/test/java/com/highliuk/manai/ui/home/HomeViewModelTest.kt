package com.highliuk.manai.ui.home

import app.cash.turbine.test
import com.highliuk.manai.data.pdf.PdfMetadataExtractor
import com.highliuk.manai.domain.model.Manga
import com.highliuk.manai.domain.repository.MangaRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val repository = mockk<MangaRepository>(relaxed = true)
    private val pdfExtractor = mockk<PdfMetadataExtractor>(relaxed = true)
    private val mangaFlow = MutableStateFlow<List<Manga>>(emptyList())

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        coEvery { repository.getAllManga() } returns mangaFlow
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() = HomeViewModel(repository, pdfExtractor)

    @Test
    fun `mangaList emits empty list initially`() = runTest(testDispatcher) {
        val viewModel = createViewModel()

        viewModel.mangaList.test {
            assertEquals(emptyList<Manga>(), awaitItem())
        }
    }

    @Test
    fun `mangaList emits data from repository`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        val mangas = listOf(
            Manga(id = 1, uri = "uri1", title = "Manga 1", pageCount = 10)
        )

        viewModel.mangaList.test {
            assertEquals(emptyList<Manga>(), awaitItem())
            mangaFlow.value = mangas
            assertEquals(mangas, awaitItem())
        }
    }

    @Test
    fun `importManga extracts page count and inserts manga`() = runTest(testDispatcher) {
        coEvery { pdfExtractor.extractPageCount("content://test.pdf") } returns 42
        val viewModel = createViewModel()

        viewModel.importManga("content://test.pdf", "my-manga.pdf")
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify {
            repository.insertManga(match {
                it.uri == "content://test.pdf" &&
                    it.title == "my-manga" &&
                    it.pageCount == 42
            })
        }
    }

    @Test
    fun `importManga strips pdf extension from title`() = runTest(testDispatcher) {
        coEvery { pdfExtractor.extractPageCount("content://file.pdf") } returns 10
        val viewModel = createViewModel()

        viewModel.importManga("content://file.pdf", "One Piece Vol.1.pdf")
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify {
            repository.insertManga(match { it.title == "One Piece Vol.1" })
        }
    }

    @Test
    fun `importManga does not crash when extractPageCount throws`() = runTest(testDispatcher) {
        coEvery { pdfExtractor.extractPageCount(any()) } throws RuntimeException("Cannot open PDF")
        val viewModel = createViewModel()

        viewModel.importManga("content://bad.pdf", "bad.pdf")
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 0) { repository.insertManga(any()) }
    }

    @Test
    fun `importManga works with filename without extension`() = runTest(testDispatcher) {
        coEvery { pdfExtractor.extractPageCount("content://noext") } returns 5
        val viewModel = createViewModel()

        viewModel.importManga("content://noext", "my-manga")
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify {
            repository.insertManga(match { it.title == "my-manga" && it.pageCount == 5 })
        }
    }
}
