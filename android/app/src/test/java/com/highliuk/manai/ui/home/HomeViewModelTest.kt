package com.highliuk.manai.ui.home

import app.cash.turbine.test
import com.highliuk.manai.data.hash.FileHashProvider
import com.highliuk.manai.data.pdf.PdfMetadataExtractor
import com.highliuk.manai.domain.model.Manga
import com.highliuk.manai.domain.repository.MangaRepository
import com.highliuk.manai.domain.repository.UserPreferencesRepository
import io.mockk.coEvery
import io.mockk.every
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
    private val fileHashProvider = mockk<FileHashProvider>(relaxed = true)
    private val userPreferencesRepository = mockk<UserPreferencesRepository>(relaxed = true)
    private val mangaFlow = MutableStateFlow<List<Manga>>(emptyList())
    private val gridColumnsFlow = MutableStateFlow(2)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        coEvery { repository.getAllManga() } returns mangaFlow
        every { userPreferencesRepository.gridColumns } returns gridColumnsFlow
        coEvery { fileHashProvider.computeHash(uri = any()) } returns "defaulthash"
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() = HomeViewModel(
        repository, pdfExtractor, fileHashProvider, userPreferencesRepository
    )

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
    fun `importManga extracts page count and inserts manga with content hash`() = runTest(testDispatcher) {
        coEvery { pdfExtractor.extractPageCount("content://test.pdf") } returns 42
        coEvery { fileHashProvider.computeHash(uri = "content://test.pdf") } returns "abc123"
        val viewModel = createViewModel()

        viewModel.importManga("content://test.pdf", "my-manga.pdf")
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify {
            repository.upsertManga(match {
                it.uri == "content://test.pdf" &&
                    it.title == "my-manga" &&
                    it.pageCount == 42 &&
                    it.contentHash == "abc123"
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
            repository.upsertManga(match { it.title == "One Piece Vol.1" })
        }
    }

    @Test
    fun `importManga does not crash when extractPageCount throws`() = runTest(testDispatcher) {
        coEvery { pdfExtractor.extractPageCount(any()) } throws RuntimeException("Cannot open PDF")
        val viewModel = createViewModel()

        viewModel.importManga("content://bad.pdf", "bad.pdf")
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 0) { repository.upsertManga(any()) }
    }

    @Test
    fun `importManga works with filename without extension`() = runTest(testDispatcher) {
        coEvery { pdfExtractor.extractPageCount("content://noext") } returns 5
        val viewModel = createViewModel()

        viewModel.importManga("content://noext", "my-manga")
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify {
            repository.upsertManga(match { it.title == "my-manga" && it.pageCount == 5 })
        }
    }

    @Test
    fun `importManga emits navigation event with manga id`() = runTest(testDispatcher) {
        coEvery { pdfExtractor.extractPageCount("content://test.pdf") } returns 42
        coEvery { repository.upsertManga(any()) } returns 7L
        val viewModel = createViewModel()

        viewModel.navigateToReader.test {
            viewModel.importManga("content://test.pdf", "my-manga.pdf")
            testDispatcher.scheduler.advanceUntilIdle()
            assertEquals(7L, awaitItem())
        }
    }

    @Test
    fun `importManga does not emit navigation event on failure`() = runTest(testDispatcher) {
        coEvery { pdfExtractor.extractPageCount(any()) } throws RuntimeException("fail")
        val viewModel = createViewModel()

        viewModel.navigateToReader.test {
            viewModel.importManga("content://bad.pdf", "bad.pdf")
            testDispatcher.scheduler.advanceUntilIdle()
            expectNoEvents()
        }
    }

    @Test
    fun `importManga navigates to existing manga when duplicate content hash`() = runTest(testDispatcher) {
        coEvery { pdfExtractor.extractPageCount("content://duplicate.pdf") } returns 10
        coEvery { fileHashProvider.computeHash(uri = "content://duplicate.pdf") } returns "existinghash"
        coEvery { repository.upsertManga(any()) } returns 42L
        val viewModel = createViewModel()

        viewModel.navigateToReader.test {
            viewModel.importManga("content://duplicate.pdf", "duplicate.pdf")
            testDispatcher.scheduler.advanceUntilIdle()
            assertEquals(42L, awaitItem())
        }
    }

    @Test
    fun `gridColumns emits value from preferences repository`() = runTest(testDispatcher) {
        val viewModel = createViewModel()

        viewModel.gridColumns.test {
            assertEquals(2, awaitItem())
            gridColumnsFlow.value = 3
            assertEquals(3, awaitItem())
        }
    }
}
