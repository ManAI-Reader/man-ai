package com.highliuk.manai.ui.settings

import app.cash.turbine.test
import com.highliuk.manai.data.llm.LlmCredentialsManager
import com.highliuk.manai.data.translation.DeepLCredentialsManager
import com.highliuk.manai.domain.model.AppLanguage
import com.highliuk.manai.domain.model.ReadingMode
import com.highliuk.manai.domain.model.TargetLanguage
import com.highliuk.manai.domain.model.ThemeMode
import com.highliuk.manai.domain.repository.UserPreferencesRepository
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
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
    private val translationTargetLangFlow = MutableStateFlow(TargetLanguage.EN)
    private val credentialsManager = mockk<DeepLCredentialsManager>(relaxed = true)
    private val llmCredentialsManager = mockk<LlmCredentialsManager>(relaxed = true)
    private val llmBaseUrlFlow = MutableStateFlow("https://api.groq.com/openai/v1")
    private val llmModelFlow = MutableStateFlow("llama-3.3-70b-versatile")
    private val gridColumnsFlow = MutableStateFlow(2)
    private val readingModeFlow = MutableStateFlow(ReadingMode.LTR)
    private val themeModeFlow = MutableStateFlow(ThemeMode.SYSTEM)
    private val appLanguageFlow = MutableStateFlow(AppLanguage.SYSTEM)
    private val ocrFontScaleFlow = MutableStateFlow(1.5f)
    private val tapToNavigatePortraitFlow = MutableStateFlow(false)
    private val tapToNavigateLandscapeFlow = MutableStateFlow(true)
    private val gridColumnsLandscapeFlow = MutableStateFlow(5)
    private val showFuriganaFlow = MutableStateFlow(false)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { userPreferencesRepository.gridColumns } returns gridColumnsFlow
        every { userPreferencesRepository.gridColumnsLandscape } returns gridColumnsLandscapeFlow
        every { userPreferencesRepository.readingMode } returns readingModeFlow
        every { userPreferencesRepository.themeMode } returns themeModeFlow
        every { userPreferencesRepository.appLanguage } returns appLanguageFlow
        every { userPreferencesRepository.ocrFontScale } returns ocrFontScaleFlow
        every { userPreferencesRepository.tapToNavigatePortrait } returns tapToNavigatePortraitFlow
        every { userPreferencesRepository.tapToNavigateLandscape } returns tapToNavigateLandscapeFlow
        every { userPreferencesRepository.translationTargetLang } returns translationTargetLangFlow
        every { userPreferencesRepository.showFurigana } returns showFuriganaFlow
        every { userPreferencesRepository.llmBaseUrl } returns llmBaseUrlFlow
        every { userPreferencesRepository.llmModel } returns llmModelFlow
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() =
        SettingsViewModel(userPreferencesRepository, credentialsManager, llmCredentialsManager)

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
    fun `ocrFontScale emits current preference value`() = runTest(testDispatcher) {
        val viewModel = createViewModel()

        viewModel.ocrFontScale.test {
            assertEquals(1.5f, awaitItem())
            ocrFontScaleFlow.value = 3.0f
            assertEquals(3.0f, awaitItem())
        }
    }

    @Test
    fun `setOcrFontScale updates preference`() = runTest(testDispatcher) {
        val viewModel = createViewModel()

        viewModel.setOcrFontScale(3.0f)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { userPreferencesRepository.setOcrFontScale(3.0f) }
    }

    @Test
    fun `tapToNavigatePortrait emits current preference value`() = runTest(testDispatcher) {
        val viewModel = createViewModel()

        viewModel.tapToNavigatePortrait.test {
            assertEquals(false, awaitItem())
            tapToNavigatePortraitFlow.value = true
            assertEquals(true, awaitItem())
        }
    }

    @Test
    fun `setTapToNavigatePortrait updates preference`() = runTest(testDispatcher) {
        val viewModel = createViewModel()

        viewModel.setTapToNavigatePortrait(true)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { userPreferencesRepository.setTapToNavigatePortrait(true) }
    }

    @Test
    fun `tapToNavigateLandscape emits current preference value`() = runTest(testDispatcher) {
        val viewModel = createViewModel()

        viewModel.tapToNavigateLandscape.test {
            assertEquals(true, awaitItem())
            tapToNavigateLandscapeFlow.value = false
            assertEquals(false, awaitItem())
        }
    }

    @Test
    fun `setTapToNavigateLandscape updates preference`() = runTest(testDispatcher) {
        val viewModel = createViewModel()

        viewModel.setTapToNavigateLandscape(false)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { userPreferencesRepository.setTapToNavigateLandscape(false) }
    }

    @Test
    fun `gridColumnsLandscape emits current preference value`() = runTest(testDispatcher) {
        val viewModel = createViewModel()

        viewModel.gridColumnsLandscape.test {
            assertEquals(5, awaitItem())
            gridColumnsLandscapeFlow.value = 6
            assertEquals(6, awaitItem())
        }
    }

    @Test
    fun `setGridColumnsLandscape updates preference`() = runTest(testDispatcher) {
        val viewModel = createViewModel()

        viewModel.setGridColumnsLandscape(4)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { userPreferencesRepository.setGridColumnsLandscape(4) }
    }

    @Test
    fun `translationTargetLang emits current preference value`() = runTest(testDispatcher) {
        val viewModel = createViewModel()

        viewModel.translationTargetLang.test {
            assertEquals(TargetLanguage.EN, awaitItem())
            translationTargetLangFlow.value = TargetLanguage.IT
            assertEquals(TargetLanguage.IT, awaitItem())
        }
    }

    @Test
    fun `setTranslationTargetLang updates preference`() = runTest(testDispatcher) {
        val viewModel = createViewModel()

        viewModel.setTranslationTargetLang(TargetLanguage.IT)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { userPreferencesRepository.setTranslationTargetLang(TargetLanguage.IT) }
    }

    @Test
    fun `deeplApiKey emits value from credentials manager`() = runTest(testDispatcher) {
        every { credentialsManager.getApiKey() } returns "test-key"

        val viewModel = createViewModel()

        assertEquals("test-key", viewModel.deeplApiKey.value)
    }

    @Test
    fun `setDeeplApiKey saves key via credentials manager`() = runTest(testDispatcher) {
        val viewModel = createViewModel()

        viewModel.setDeeplApiKey("new-key")

        verify { credentialsManager.saveApiKey("new-key") }
    }

    @Test
    fun `setDeeplApiKey clears when blank`() = runTest(testDispatcher) {
        val viewModel = createViewModel()

        viewModel.setDeeplApiKey("")

        verify { credentialsManager.clearApiKey() }
    }

    @Test
    fun `llmApiKey emits value from credentials manager`() = runTest(testDispatcher) {
        every { llmCredentialsManager.getApiKey() } returns "llm-key"

        val viewModel = createViewModel()

        assertEquals("llm-key", viewModel.llmApiKey.value)
    }

    @Test
    fun `setLlmApiKey saves key via credentials manager`() = runTest(testDispatcher) {
        val viewModel = createViewModel()

        viewModel.setLlmApiKey("new-llm-key")

        verify { llmCredentialsManager.saveApiKey("new-llm-key") }
        assertEquals("new-llm-key", viewModel.llmApiKey.value)
    }

    @Test
    fun `setLlmApiKey clears when blank`() = runTest(testDispatcher) {
        every { llmCredentialsManager.getApiKey() } returns "old-key"
        val viewModel = createViewModel()

        viewModel.setLlmApiKey("")

        verify { llmCredentialsManager.clearApiKey() }
        assertEquals("", viewModel.llmApiKey.value)
    }

    @Test
    fun `llmBaseUrl seeds from stored preference`() = runTest(testDispatcher) {
        llmBaseUrlFlow.value = "https://stored.example.com/v1"

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("https://stored.example.com/v1", viewModel.llmBaseUrl.value)
    }

    @Test
    fun `setLlmBaseUrl updates flow synchronously and persists`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.setLlmBaseUrl("https://example.com/v1")

        assertEquals("https://example.com/v1", viewModel.llmBaseUrl.value)
        testDispatcher.scheduler.advanceUntilIdle()
        coVerify { userPreferencesRepository.setLlmBaseUrl("https://example.com/v1") }
    }

    @Test
    fun `llmModel seeds from stored preference`() = runTest(testDispatcher) {
        llmModelFlow.value = "stored-model"

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("stored-model", viewModel.llmModel.value)
    }

    @Test
    fun `setLlmModel updates flow synchronously and persists`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.setLlmModel("gemma2-9b-it")

        assertEquals("gemma2-9b-it", viewModel.llmModel.value)
        testDispatcher.scheduler.advanceUntilIdle()
        coVerify { userPreferencesRepository.setLlmModel("gemma2-9b-it") }
    }

    @Test
    fun `showFurigana emits current preference value`() = runTest(testDispatcher) {
        val viewModel = createViewModel()

        viewModel.showFurigana.test {
            assertEquals(false, awaitItem())
            showFuriganaFlow.value = true
            assertEquals(true, awaitItem())
        }
    }

    @Test
    fun `setShowFurigana updates preference`() = runTest(testDispatcher) {
        val viewModel = createViewModel()

        viewModel.setShowFurigana(true)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { userPreferencesRepository.setShowFurigana(true) }
    }
}
