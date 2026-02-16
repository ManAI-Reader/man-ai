package com.highliuk.manai.ui.reader

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.highliuk.manai.domain.model.Manga
import com.highliuk.manai.domain.model.ReadingMode
import com.highliuk.manai.domain.repository.MangaRepository
import com.highliuk.manai.domain.repository.UserPreferencesRepository
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
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ReaderViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val repository = mockk<MangaRepository>(relaxed = true)
    private val userPreferencesRepository = mockk<UserPreferencesRepository>(relaxed = true)
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
        return ReaderViewModel(savedStateHandle, repository, userPreferencesRepository)
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
    fun `readingMode emits value from repository`() = runTest(testDispatcher) {
        coEvery { repository.getMangaById(1L) } returns flowOf(null)
        val viewModel = createViewModel(1L)

        viewModel.readingMode.test {
            assertEquals(ReadingMode.LTR, awaitItem())
            readingModeFlow.value = ReadingMode.RTL
            assertEquals(ReadingMode.RTL, awaitItem())
        }
    }
}
