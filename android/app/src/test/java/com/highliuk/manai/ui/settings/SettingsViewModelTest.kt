package com.highliuk.manai.ui.settings

import app.cash.turbine.test
import com.highliuk.manai.domain.model.AppLanguage
import com.highliuk.manai.domain.model.ReadingMode
import com.highliuk.manai.domain.model.ThemeMode
import com.highliuk.manai.domain.repository.UserPreferencesRepository
import io.mockk.coVerify
import io.mockk.every
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
class SettingsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val userPreferencesRepository = mockk<UserPreferencesRepository>(relaxed = true)
    private val gridColumnsFlow = MutableStateFlow(2)
    private val readingModeFlow = MutableStateFlow(ReadingMode.LTR)
    private val themeModeFlow = MutableStateFlow(ThemeMode.SYSTEM)
    private val appLanguageFlow = MutableStateFlow(AppLanguage.SYSTEM)
    private val tapToNavigateFlow = MutableStateFlow(false)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { userPreferencesRepository.gridColumns } returns gridColumnsFlow
        every { userPreferencesRepository.readingMode } returns readingModeFlow
        every { userPreferencesRepository.themeMode } returns themeModeFlow
        every { userPreferencesRepository.appLanguage } returns appLanguageFlow
        every { userPreferencesRepository.tapToNavigate } returns tapToNavigateFlow
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() = SettingsViewModel(userPreferencesRepository)

    @Test
    fun `gridColumns emits current preference value`() = runTest(testDispatcher) {
        val viewModel = createViewModel()

        viewModel.gridColumns.test {
            assertEquals(2, awaitItem())
            gridColumnsFlow.value = 3
            assertEquals(3, awaitItem())
        }
    }

    @Test
    fun `setGridColumns updates preference`() = runTest(testDispatcher) {
        val viewModel = createViewModel()

        viewModel.setGridColumns(3)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { userPreferencesRepository.setGridColumns(3) }
    }

    @Test
    fun `readingMode emits current preference value`() = runTest(testDispatcher) {
        val viewModel = createViewModel()

        viewModel.readingMode.test {
            assertEquals(ReadingMode.LTR, awaitItem())
            readingModeFlow.value = ReadingMode.RTL
            assertEquals(ReadingMode.RTL, awaitItem())
        }
    }

    @Test
    fun `setReadingMode updates preference`() = runTest(testDispatcher) {
        val viewModel = createViewModel()

        viewModel.setReadingMode(ReadingMode.RTL)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { userPreferencesRepository.setReadingMode(ReadingMode.RTL) }
    }

    @Test
    fun `themeMode emits current preference value`() = runTest(testDispatcher) {
        val viewModel = createViewModel()

        viewModel.themeMode.test {
            assertEquals(ThemeMode.SYSTEM, awaitItem())
            themeModeFlow.value = ThemeMode.DARK
            assertEquals(ThemeMode.DARK, awaitItem())
        }
    }

    @Test
    fun `setThemeMode updates preference`() = runTest(testDispatcher) {
        val viewModel = createViewModel()

        viewModel.setThemeMode(ThemeMode.DARK)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { userPreferencesRepository.setThemeMode(ThemeMode.DARK) }
    }

    @Test
    fun `appLanguage emits current preference value`() = runTest(testDispatcher) {
        val viewModel = createViewModel()

        viewModel.appLanguage.test {
            assertEquals(AppLanguage.SYSTEM, awaitItem())
            appLanguageFlow.value = AppLanguage.ITALIAN
            assertEquals(AppLanguage.ITALIAN, awaitItem())
        }
    }

    @Test
    fun `setAppLanguage updates preference`() = runTest(testDispatcher) {
        val viewModel = createViewModel()

        viewModel.setAppLanguage(AppLanguage.ITALIAN)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { userPreferencesRepository.setAppLanguage(AppLanguage.ITALIAN) }
    }

    @Test
    fun `tapToNavigate emits current preference value`() = runTest(testDispatcher) {
        val viewModel = createViewModel()

        viewModel.tapToNavigate.test {
            assertEquals(false, awaitItem())
            tapToNavigateFlow.value = true
            assertEquals(true, awaitItem())
        }
    }

    @Test
    fun `setTapToNavigate updates preference`() = runTest(testDispatcher) {
        val viewModel = createViewModel()

        viewModel.setTapToNavigate(true)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { userPreferencesRepository.setTapToNavigate(true) }
    }
}
