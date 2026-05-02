package com.highliuk.manai.ui.reader

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.highliuk.manai.data.pdf.PdfPageRenderer
import com.highliuk.manai.domain.debug.DebugMlEventHolder
import com.highliuk.manai.domain.debug.PipelineDebugStateHolder
import com.highliuk.manai.domain.model.Manga
import com.highliuk.manai.domain.model.PagePipelineState
import com.highliuk.manai.domain.model.PageRegion
import com.highliuk.manai.domain.model.ReadingMode
import com.highliuk.manai.domain.model.TranslationResult
import com.highliuk.manai.domain.repository.MangaRepository
import com.highliuk.manai.domain.repository.OcrCacheRepository
import com.highliuk.manai.domain.repository.TranslationRepository
import com.highliuk.manai.domain.repository.UserPreferencesRepository
import com.highliuk.manai.domain.usecase.ProcessPageUseCase
import android.graphics.Bitmap
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.awaitCancellation
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
    private val translationRepository = mockk<TranslationRepository>(relaxed = true)
    private val pdfPageRenderer = mockk<PdfPageRenderer>(relaxed = true)
    private val debugStateHolder = PipelineDebugStateHolder()
    private val debugEventHolder = DebugMlEventHolder()
    private val readingModeFlow = MutableStateFlow(ReadingMode.LTR)
    private val tapToNavigatePortraitFlow = MutableStateFlow(false)
    private val tapToNavigateLandscapeFlow = MutableStateFlow(true)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { userPreferencesRepository.readingMode } returns readingModeFlow
        every { userPreferencesRepository.ocrFontScale } returns MutableStateFlow(1.5f)
        every { userPreferencesRepository.tapToNavigatePortrait } returns tapToNavigatePortraitFlow
        every { userPreferencesRepository.tapToNavigateLandscape } returns tapToNavigateLandscapeFlow
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
            debugStateHolder, debugEventHolder, translationRepository,
        )
    }

    @Test
    fun `manga emits value from repository`() = runTest(testDispatcher) {
        val manga = Manga(id = 1, uri = "uri1", title = "Manga 1", pageCount = 200)
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
    fun `tapToNavigatePortrait emits value from preferences`() = runTest(testDispatcher) {
        coEvery { repository.getMangaById(1L) } returns flowOf(
            Manga(id = 1, uri = "uri1", title = "Test", pageCount = 10)
        )

        val viewModel = createViewModel(1L)

        viewModel.tapToNavigatePortrait.test {
            assertEquals(false, awaitItem())
            tapToNavigatePortraitFlow.value = true
            assertEquals(true, awaitItem())
        }
    }

    @Test
    fun `tapToNavigateLandscape emits value from preferences`() = runTest(testDispatcher) {
        coEvery { repository.getMangaById(1L) } returns flowOf(
            Manga(id = 1, uri = "uri1", title = "Test", pageCount = 10)
        )

        val viewModel = createViewModel(1L)

        viewModel.tapToNavigateLandscape.test {
            assertEquals(true, awaitItem())
            tapToNavigateLandscapeFlow.value = false
            assertEquals(false, awaitItem())
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

    @Test
    fun `debugPipelineStates exposes state holder states`() = runTest(testDispatcher) {
        coEvery { repository.getMangaById(1L) } returns flowOf(null)
        every { ocrCache.observeRegions(any(), any()) } returns flowOf(emptyList())

        val viewModel = createViewModel(1L)

        viewModel.debugPipelineStates.test {
            assertEquals(emptyMap<Int, PagePipelineState>(), awaitItem())
        }
    }

    @Test
    fun `debugEvents exposes event holder events`() = runTest(testDispatcher) {
        coEvery { repository.getMangaById(1L) } returns flowOf(null)
        every { ocrCache.observeRegions(any(), any()) } returns flowOf(emptyList())

        val viewModel = createViewModel(1L)

        org.junit.Assert.assertNotNull(viewModel.debugEvents)
    }

    @Test
    fun `onVisiblePagesChanged launches pipeline for each visible page`() = runTest(testDispatcher) {
        val manga = Manga(id = 1, uri = "content://test", title = "Test", pageCount = 10)
        coEvery { repository.getMangaById(1L) } returns flowOf(manga)
        every { ocrCache.observeRegions(any(), any()) } returns flowOf(emptyList())
        coEvery { pdfPageRenderer.render(any(), any()) } returns mockk(relaxed = true)

        val viewModel = createViewModel(1L)
        testScheduler.advanceUntilIdle()
        clearMocks(processPageUseCase, answers = false)

        viewModel.onVisiblePagesChanged(listOf(2, 3, 4))
        testScheduler.advanceUntilIdle()

        coVerify { processPageUseCase.execute(1L, 2, any(), any(), any()) }
        coVerify { processPageUseCase.execute(1L, 3, any(), any(), any()) }
        coVerify { processPageUseCase.execute(1L, 4, any(), any(), any()) }
    }

    @Test
    fun `onVisiblePagesChanged does not relaunch already-active pipelines`() = runTest(testDispatcher) {
        val manga = Manga(id = 1, uri = "content://test", title = "Test", pageCount = 10)
        coEvery { repository.getMangaById(1L) } returns flowOf(manga)
        every { ocrCache.observeRegions(any(), any()) } returns flowOf(emptyList())
        coEvery { pdfPageRenderer.render(any(), any()) } returns mockk(relaxed = true)

        val viewModel = createViewModel(1L)
        testScheduler.advanceUntilIdle()
        clearMocks(processPageUseCase, answers = false, verificationMarks = true)
        coEvery { processPageUseCase.execute(any(), any(), any(), any(), any()) } coAnswers {
            awaitCancellation()
        }

        viewModel.onVisiblePagesChanged(listOf(2, 3))
        testScheduler.advanceUntilIdle()

        viewModel.onVisiblePagesChanged(listOf(2, 3))
        testScheduler.advanceUntilIdle()

        coVerify(exactly = 1) { processPageUseCase.execute(1L, 2, any(), any(), any()) }
        coVerify(exactly = 1) { processPageUseCase.execute(1L, 3, any(), any(), any()) }
    }

    @Test
    fun `visiblePagesRegions emits regions for tracked visible pages`() = runTest(testDispatcher) {
        val manga = Manga(id = 1, uri = "uri1", title = "Test", pageCount = 10)
        coEvery { repository.getMangaById(1L) } returns flowOf(manga)
        coEvery { pdfPageRenderer.render(any(), any()) } returns mockk(relaxed = true)

        val regionsPage2 = listOf(PageRegion(0, 0.1f, 0.1f, 0.5f, 0.5f, 0.9f, "hello"))
        val regionsPage3 = listOf(PageRegion(0, 0.2f, 0.2f, 0.6f, 0.6f, 0.8f, "world"))
        every { ocrCache.observeRegions(1L, 2) } returns flowOf(regionsPage2)
        every { ocrCache.observeRegions(1L, 3) } returns flowOf(regionsPage3)
        every { ocrCache.observeRegions(1L, match { it != 2 && it != 3 }) } returns flowOf(emptyList())

        val viewModel = createViewModel(1L)
        testScheduler.advanceUntilIdle()

        viewModel.visiblePagesRegions.test {
            viewModel.onVisiblePagesChanged(listOf(2, 3))
            testScheduler.advanceUntilIdle()

            val map = expectMostRecentItem()
            assertEquals(regionsPage2, map[2])
            assertEquals(regionsPage3, map[3])
        }
    }

    @Test
    fun `onRegionTapped with null ocrText launches pipeline on region pageIndex not currentPage`() =
        runTest(testDispatcher) {
            val manga = Manga(id = 1, uri = "content://test", title = "Test", pageCount = 20)
            coEvery { repository.getMangaById(1L) } returns flowOf(manga)
            every { ocrCache.observeRegions(any(), any()) } returns flowOf(emptyList())
            coEvery { pdfPageRenderer.render(any(), any()) } returns mockk(relaxed = true)

            val viewModel = createViewModel(1L)
            advanceTimeBy(400)
            testScheduler.advanceUntilIdle()

            // Current page is 0 (from init). Tap a region that lives on page 5.
            clearMocks(processPageUseCase, answers = false)

            val regionOnPage5 = PageRegion(
                regionIndex = 2, normX1 = 0.1f, normY1 = 0.1f,
                normX2 = 0.5f, normY2 = 0.5f, confidence = 0.9f,
                ocrText = null, pageIndex = 5,
            )
            viewModel.onRegionTapped(regionOnPage5)
            testScheduler.advanceUntilIdle()

            // Pipeline must be launched for page 5, NOT page 0
            coVerify {
                processPageUseCase.execute(
                    1L, 5, any(), detectionOnly = false, priorityRegionIndex = 2
                )
            }
            coVerify(exactly = 0) {
                processPageUseCase.execute(
                    1L, 0, any(), detectionOnly = false, priorityRegionIndex = 2
                )
            }
        }

    @Test
    fun `readingMode emits WEBTOON when set`() = runTest(testDispatcher) {
        coEvery { repository.getMangaById(1L) } returns flowOf(null)
        val viewModel = createViewModel(1L)

        viewModel.readingMode.test {
            assertEquals(ReadingMode.LTR, awaitItem())
            readingModeFlow.value = ReadingMode.WEBTOON
            assertEquals(ReadingMode.WEBTOON, awaitItem())
        }
    }

    @Test
    fun `translationState starts as Idle`() = runTest(testDispatcher) {
        coEvery { repository.getMangaById(1L) } returns flowOf(null)
        every { ocrCache.observeRegions(any(), any()) } returns flowOf(emptyList())
        val viewModel = createViewModel(1L)

        assertEquals(ReaderViewModel.TranslationState.Idle, viewModel.translationState.value)
    }

    @Test
    fun `translateSelectedRegion moves to Loading then Translated`() = runTest(testDispatcher) {
        coEvery { repository.getMangaById(1L) } returns flowOf(null)
        every { ocrCache.observeRegions(any(), any()) } returns flowOf(emptyList())
        coEvery {
            translationRepository.getCachedTranslation(any(), any(), any(), any())
        } returns null
        coEvery {
            translationRepository.translate(1L, 0, 0, "テスト")
        } returns TranslationResult.Success("Test")

        val viewModel = createViewModel(1L)
        val region = PageRegion(0, 0.1f, 0.1f, 0.5f, 0.5f, 0.9f, "テスト", pageIndex = 0)
        viewModel.onRegionTapped(region)

        viewModel.translationState.test {
            val current = awaitItem()
            viewModel.translateSelectedRegion()
            if (current == ReaderViewModel.TranslationState.Idle) {
                assertEquals(ReaderViewModel.TranslationState.Loading, awaitItem())
            }
            assertEquals(
                ReaderViewModel.TranslationState.Translated("Test"),
                awaitItem()
            )
        }
    }

    @Test
    fun `translateSelectedRegion moves to Error on failure`() = runTest(testDispatcher) {
        coEvery { repository.getMangaById(1L) } returns flowOf(null)
        every { ocrCache.observeRegions(any(), any()) } returns flowOf(emptyList())
        coEvery {
            translationRepository.getCachedTranslation(any(), any(), any(), any())
        } returns null
        coEvery {
            translationRepository.translate(1L, 0, 0, "テスト")
        } returns TranslationResult.Error("API key missing")

        val viewModel = createViewModel(1L)
        val region = PageRegion(0, 0.1f, 0.1f, 0.5f, 0.5f, 0.9f, "テスト", pageIndex = 0)
        viewModel.onRegionTapped(region)

        viewModel.translateSelectedRegion()
        testScheduler.advanceUntilIdle()

        val state = viewModel.translationState.value
        assertEquals(ReaderViewModel.TranslationState.Error("API key missing"), state)
    }

    @Test
    fun `dismissBottomSheet resets translationState to Idle`() = runTest(testDispatcher) {
        coEvery { repository.getMangaById(1L) } returns flowOf(null)
        every { ocrCache.observeRegions(any(), any()) } returns flowOf(emptyList())
        coEvery {
            translationRepository.getCachedTranslation(any(), any(), any(), any())
        } returns null
        coEvery {
            translationRepository.translate(any(), any(), any(), any())
        } returns TranslationResult.Success("Test")

        val viewModel = createViewModel(1L)
        val region = PageRegion(0, 0.1f, 0.1f, 0.5f, 0.5f, 0.9f, "テスト", pageIndex = 0)
        viewModel.onRegionTapped(region)
        viewModel.translateSelectedRegion()
        testScheduler.advanceUntilIdle()

        viewModel.dismissBottomSheet()

        assertEquals(ReaderViewModel.TranslationState.Idle, viewModel.translationState.value)
    }

    @Test
    fun `onRegionTapped loads cached translation automatically`() = runTest(testDispatcher) {
        coEvery { repository.getMangaById(1L) } returns flowOf(null)
        every { ocrCache.observeRegions(any(), any()) } returns flowOf(emptyList())
        coEvery {
            translationRepository.getCachedTranslation(1L, 0, 0, "テスト")
        } returns "Test"

        val viewModel = createViewModel(1L)
        val region = PageRegion(0, 0.1f, 0.1f, 0.5f, 0.5f, 0.9f, "テスト", pageIndex = 0)
        viewModel.onRegionTapped(region)
        testScheduler.advanceUntilIdle()

        assertEquals(
            ReaderViewModel.TranslationState.Translated("Test"),
            viewModel.translationState.value
        )
    }
}
