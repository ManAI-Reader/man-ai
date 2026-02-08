package com.highliuk.manai.ui.home

import com.highliuk.manai.domain.model.Manga
import com.highliuk.manai.domain.model.PdfMetadata
import com.highliuk.manai.domain.repository.PdfDocumentHandler
import com.highliuk.manai.domain.usecase.GetMangaListUseCase
import com.highliuk.manai.domain.usecase.ImportMangaUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val getMangaList = mockk<GetMangaListUseCase>()
    private val importManga = mockk<ImportMangaUseCase>()
    private val pdfDocumentHandler = mockk<PdfDocumentHandler>()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): HomeViewModel {
        return HomeViewModel(getMangaList, importManga, pdfDocumentHandler)
    }

    @Test
    fun `initial state is Loading`() = runTest(testDispatcher) {
        every { getMangaList() } returns flowOf(emptyList())

        val viewModel = createViewModel()

        assertEquals(HomeUiState.Loading, viewModel.uiState.value)
    }

    @Test
    fun `state is Empty when manga list is empty`() = runTest(testDispatcher) {
        every { getMangaList() } returns flowOf(emptyList())

        val viewModel = createViewModel()
        val job = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        assertEquals(HomeUiState.Empty, viewModel.uiState.value)
        job.cancel()
    }

    @Test
    fun `state is Success when manga list is not empty`() = runTest(testDispatcher) {
        val mangaList = listOf(
            Manga(id = 1, title = "Test", filePath = "/test.pdf", pageCount = 50),
        )
        every { getMangaList() } returns flowOf(mangaList)

        val viewModel = createViewModel()
        val job = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is HomeUiState.Success)
        assertEquals(mangaList, (state as HomeUiState.Success).mangaList)
        job.cancel()
    }

    @Test
    fun `importManga extracts metadata and delegates to use case`() = runTest(testDispatcher) {
        val testUri = "content://com.android.providers.media/test.pdf"
        every { getMangaList() } returns flowOf(emptyList())
        coEvery { pdfDocumentHandler.importDocument(testUri) } returns PdfMetadata("test", 15)
        coEvery { importManga("test", testUri, 15) } returns 1L

        val viewModel = createViewModel()
        viewModel.importManga(testUri)
        advanceUntilIdle()

        coVerify { pdfDocumentHandler.importDocument(testUri) }
        coVerify { importManga("test", testUri, 15) }
    }

    @Test
    fun `importManga handles handler failure gracefully`() = runTest(testDispatcher) {
        val testUri = "content://invalid"
        every { getMangaList() } returns flowOf(emptyList())
        coEvery { pdfDocumentHandler.importDocument(testUri) } throws IllegalStateException("Cannot open PDF")

        val viewModel = createViewModel()
        // Should not crash
        viewModel.importManga(testUri)
        advanceUntilIdle()

        coVerify { pdfDocumentHandler.importDocument(testUri) }
        // Use case should NOT be called when handler fails
        coVerify(exactly = 0) { importManga(any(), any(), any()) }
    }
}
