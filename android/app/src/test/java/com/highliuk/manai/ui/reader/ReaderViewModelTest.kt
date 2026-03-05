package com.highliuk.manai.ui.reader

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.highliuk.manai.data.pdf.PdfPageRenderer
import com.highliuk.manai.domain.model.Manga
import com.highliuk.manai.domain.model.PageRegion
import com.highliuk.manai.domain.model.ReadingMode
import com.highliuk.manai.domain.repository.MangaRepository
import com.highliuk.manai.domain.repository.OcrCacheRepository
import com.highliuk.manai.domain.repository.UserPreferencesRepository
import com.highliuk.manai.domain.usecase.ProcessPageUseCase
import android.graphics.Bitmap
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ReaderViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val repository = mockk<MangaRepository>(relaxed = true)
    private val userPreferencesRepository = mockk<UserPreferencesRepository>(relaxed = true)
    private val processPageUseCase = mockk<ProcessPageUseCase>(relaxed = true)
    private val ocrCache = mockk<OcrCacheRepository>(relaxed = true)
    private val pdfPageRenderer = mockk<PdfPageRenderer>(relaxed = true)
    private val readingModeFlow = MutableStateFlow(ReadingMode.LTR)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { userPreferencesRepository.readingMode } returns readingModeFlow
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(mangaId: Long = 1L): ReaderViewModel {
        val savedStateHandle = SavedStateHandle(mapOf("mangaId" to mangaId))
        return ReaderViewModel(
            savedStateHandle, repository, userPreferencesRepository,
            processPageUseCase, ocrCache, pdfPageRenderer,
        )
    }

    @Test
    fun `manga emits value from repository`() = runTest(testDispatcher) {
        val manga = Manga(id = 1, uri = "uri1", title = "One Piece", pageCount = 200)
        coEvery { repository.getMangaById(1L) } returns flowOf(manga)

        val viewModel = createViewModel(1L)

        viewModel.manga.test {
            assertEquals(null, awaitItem())
            assertEquals(manga, awaitItem())
        }
    }

    @Test
    fun `manga emits null when id not found`() = runTest(testDispatcher) {
        coEvery { repository.getMangaById(999L) } returns flowOf(null)

        val viewModel = createViewModel(999L)

        viewModel.manga.test {
            assertEquals(null, awaitItem())
        }
    }

    @Test
    fun `currentPage starts at 0 before manga loads`() = runTest(testDispatcher) {
        coEvery { repository.getMangaById(1L) } returns flowOf(null)
        val viewModel = createViewModel(1L)

        assertEquals(0, viewModel.currentPage.value)
    }

    @Test
    fun `currentPage starts at manga lastReadPage`() = runTest(testDispatcher) {
        val manga = Manga(
            id = 1, uri = "uri1", title = "Test", pageCount = 100, lastReadPage = 42
        )
        coEvery { repository.getMangaById(1L) } returns flowOf(manga)

        val viewModel = createViewModel(1L)

        viewModel.currentPage.test {
            assertEquals(0, awaitItem())
            assertEquals(42, awaitItem())
        }
    }

    @Test
    fun `onPageChanged updates currentPage`() = runTest(testDispatcher) {
        coEvery { repository.getMangaById(1L) } returns flowOf(null)
        val viewModel = createViewModel(1L)

        viewModel.onPageChanged(3)

        assertEquals(3, viewModel.currentPage.value)
    }

    @Test
    fun `onPageChanged persists page after debounce`() = runTest(testDispatcher) {
        val manga = Manga(id = 1, uri = "uri1", title = "Test", pageCount = 100)
        coEvery { repository.getMangaById(1L) } returns flowOf(manga)

        val viewModel = createViewModel(1L)
        testScheduler.advanceUntilIdle()

        viewModel.onPageChanged(5)
        advanceTimeBy(600)
        testScheduler.advanceUntilIdle()

        coVerify { repository.updateLastReadPage(1L, 5) }
    }

    @Test
    fun `rapid page changes only persist last value`() = runTest(testDispatcher) {
        val manga = Manga(id = 1, uri = "uri1", title = "Test", pageCount = 100)
        coEvery { repository.getMangaById(1L) } returns flowOf(manga)

        val viewModel = createViewModel(1L)
        testScheduler.advanceUntilIdle()

        viewModel.onPageChanged(1)
        advanceTimeBy(100)
        viewModel.onPageChanged(2)
        advanceTimeBy(100)
        viewModel.onPageChanged(3)
        advanceTimeBy(600)
        testScheduler.advanceUntilIdle()

        coVerify(exactly = 1) { repository.updateLastReadPage(1L, 3) }
        coVerify(exactly = 0) { repository.updateLastReadPage(1L, 1) }
        coVerify(exactly = 0) { repository.updateLastReadPage(1L, 2) }
    }

    @Test
    fun `readingMode emits default LTR`() = runTest(testDispatcher) {
        coEvery { repository.getMangaById(1L) } returns flowOf(null)
        val viewModel = createViewModel(1L)

        viewModel.readingMode.test {
            assertEquals(ReadingMode.LTR, awaitItem())
        }
    }

    @Test
    fun `onCleared persists current page`() = runTest(testDispatcher) {
        val manga = Manga(id = 1, uri = "uri1", title = "Test", pageCount = 100)
        coEvery { repository.getMangaById(1L) } returns flowOf(manga)

        val viewModel = createViewModel(1L)
        testScheduler.advanceUntilIdle()

        viewModel.onPageChanged(7)

        val onClearedMethod = viewModel.javaClass.getDeclaredMethod("onCleared")
        onClearedMethod.isAccessible = true
        onClearedMethod.invoke(viewModel)
        testScheduler.advanceUntilIdle()

        coVerify { repository.updateLastReadPage(1L, 7) }
    }

    @Test
    fun `readingMode emits value from repository`() = runTest(testDispatcher) {
        coEvery { repository.getMangaById(1L) } returns flowOf(null)
        val viewModel = createViewModel(1L)

        viewModel.readingMode.test {
            assertEquals(ReadingMode.LTR, awaitItem())
            readingModeFlow.value = ReadingMode.RTL
            assertEquals(ReadingMode.RTL, awaitItem())
        }
    }

    @Test
    fun `currentPageRegions emits regions for current page`() = runTest(testDispatcher) {
        val manga = Manga(id = 1, uri = "uri1", title = "Test", pageCount = 10)
        coEvery { repository.getMangaById(1L) } returns flowOf(manga)

        val regions = listOf(
            PageRegion(0, 0.1f, 0.1f, 0.5f, 0.5f, 0.9f, "hello")
        )
        every { ocrCache.observeRegions(1L, any()) } returns flowOf(emptyList())
        every { ocrCache.observeRegions(1L, 0) } returns flowOf(regions)

        val viewModel = createViewModel(1L)

        viewModel.currentPageRegions.test {
            assertEquals(emptyList<PageRegion>(), awaitItem())
            assertEquals(regions, awaitItem())
        }
    }

    @Test
    fun `onRegionTapped sets selectedRegion`() = runTest(testDispatcher) {
        coEvery { repository.getMangaById(1L) } returns flowOf(null)
        every { ocrCache.observeRegions(any(), any()) } returns flowOf(emptyList())
        val viewModel = createViewModel(1L)

        val region = PageRegion(0, 0.1f, 0.1f, 0.5f, 0.5f, 0.9f, "hello")
        viewModel.onRegionTapped(region)

        assertEquals(region, viewModel.selectedRegion.value)
    }

    @Test
    fun `dismissBottomSheet clears selectedRegion`() = runTest(testDispatcher) {
        coEvery { repository.getMangaById(1L) } returns flowOf(null)
        every { ocrCache.observeRegions(any(), any()) } returns flowOf(emptyList())
        val viewModel = createViewModel(1L)

        viewModel.onRegionTapped(PageRegion(0, 0.1f, 0.1f, 0.5f, 0.5f, 0.9f, "hi"))
        viewModel.dismissBottomSheet()

        assertNull(viewModel.selectedRegion.value)
    }

    @Test
    fun `pipeline launches on page change`() = runTest(testDispatcher) {
        val manga = Manga(id = 1, uri = "content://test", title = "Test", pageCount = 10)
        coEvery { repository.getMangaById(1L) } returns flowOf(manga)
        every { ocrCache.observeRegions(any(), any()) } returns flowOf(emptyList())
        val fakeBitmap = mockk<Bitmap>(relaxed = true)
        coEvery { pdfPageRenderer.render("content://test", 0) } returns fakeBitmap

        val viewModel = createViewModel(1L)
        advanceTimeBy(400)
        testScheduler.advanceUntilIdle()

        coVerify { pdfPageRenderer.render("content://test", 0) }
        coVerify {
            processPageUseCase.execute(1L, 0, fakeBitmap, detectionOnly = false, priorityRegionIndex = null)
        }
    }

    @Test
    fun `pipeline for previous page continues when navigating to new page`() = runTest(testDispatcher) {
        val manga = Manga(id = 1, uri = "content://test", title = "Test", pageCount = 10)
        coEvery { repository.getMangaById(1L) } returns flowOf(manga)
        every { ocrCache.observeRegions(any(), any()) } returns flowOf(emptyList())
        coEvery { pdfPageRenderer.render(any(), any()) } returns mockk(relaxed = true)

        val page0Cancelled = java.util.concurrent.atomic.AtomicBoolean(false)

        coEvery { processPageUseCase.execute(1L, 0, any(), any(), any()) } coAnswers {
            try {
                kotlinx.coroutines.awaitCancellation()
            } catch (e: kotlinx.coroutines.CancellationException) {
                page0Cancelled.set(true)
                throw e
            }
        }
        coEvery { processPageUseCase.execute(1L, 5, any(), any(), any()) } returns Unit

        val viewModel = createViewModel(1L)
        advanceTimeBy(400)
        testScheduler.advanceUntilIdle()

        viewModel.onPageChanged(5)
        advanceTimeBy(400)
        testScheduler.advanceUntilIdle()

        org.junit.Assert.assertFalse(
            "Page 0 pipeline should NOT be cancelled when navigating away",
            page0Cancelled.get()
        )
    }

    @Test
    fun `onRegionTapped with null ocrText relaunches pipeline with priority`() =
        runTest(testDispatcher) {
            val manga = Manga(id = 1, uri = "content://test", title = "Test", pageCount = 10)
            coEvery { repository.getMangaById(1L) } returns flowOf(manga)
            every { ocrCache.observeRegions(any(), any()) } returns flowOf(emptyList())
            coEvery { pdfPageRenderer.render(any(), any()) } returns mockk(relaxed = true)

            val viewModel = createViewModel(1L)
            advanceTimeBy(400)
            testScheduler.advanceUntilIdle()

            // Clear invocations from init pipeline
            clearMocks(processPageUseCase, answers = false)

            val pendingRegion = PageRegion(2, 0.5f, 0.5f, 0.8f, 0.8f, 0.9f, null)
            viewModel.onRegionTapped(pendingRegion)
            testScheduler.advanceUntilIdle()

            coVerify {
                processPageUseCase.execute(
                    1L, 0, any(), detectionOnly = false, priorityRegionIndex = 2
                )
            }
        }

    @Test
    fun `pipeline processes all visited pages not just debounced last`() = runTest(testDispatcher) {
        val manga = Manga(id = 1, uri = "content://test", title = "Test", pageCount = 10)
        coEvery { repository.getMangaById(1L) } returns flowOf(manga)
        every { ocrCache.observeRegions(any(), any()) } returns flowOf(emptyList())
        coEvery { pdfPageRenderer.render(any(), any()) } returns mockk(relaxed = true)

        val viewModel = createViewModel(1L)
        testScheduler.advanceUntilIdle()

        viewModel.onPageChanged(1)
        viewModel.onPageChanged(2)
        viewModel.onPageChanged(3)
        testScheduler.advanceUntilIdle()

        coVerify { processPageUseCase.execute(1L, 0, any(), any(), any()) }
        coVerify { processPageUseCase.execute(1L, 1, any(), any(), any()) }
        coVerify { processPageUseCase.execute(1L, 2, any(), any(), any()) }
        coVerify { processPageUseCase.execute(1L, 3, any(), any(), any()) }
    }

    @Test
    fun `pipeline does not process unvisited pages`() = runTest(testDispatcher) {
        val manga = Manga(id = 1, uri = "content://test", title = "Test", pageCount = 10)
        coEvery { repository.getMangaById(1L) } returns flowOf(manga)
        every { ocrCache.observeRegions(any(), any()) } returns flowOf(emptyList())
        coEvery { pdfPageRenderer.render(any(), any()) } returns mockk(relaxed = true)

        val viewModel = createViewModel(1L)
        testScheduler.advanceUntilIdle()

        coVerify { processPageUseCase.execute(1L, 0, any(), any(), any()) }
        coVerify(exactly = 0) { processPageUseCase.execute(1L, 1, any(), any(), any()) }
    }
}
