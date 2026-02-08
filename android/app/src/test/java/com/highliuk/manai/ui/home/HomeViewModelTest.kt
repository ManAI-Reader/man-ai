package com.highliuk.manai.ui.home

import com.highliuk.manai.domain.model.Manga
import com.highliuk.manai.domain.usecase.GetMangaListUseCase
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

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is Loading`() = runTest(testDispatcher) {
        every { getMangaList() } returns flowOf(emptyList())

        val viewModel = HomeViewModel(getMangaList)

        assertEquals(HomeUiState.Loading, viewModel.uiState.value)
    }

    @Test
    fun `state is Empty when manga list is empty`() = runTest(testDispatcher) {
        every { getMangaList() } returns flowOf(emptyList())

        val viewModel = HomeViewModel(getMangaList)
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

        val viewModel = HomeViewModel(getMangaList)
        val job = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is HomeUiState.Success)
        assertEquals(mangaList, (state as HomeUiState.Success).mangaList)
        job.cancel()
    }
}
