package com.highliuk.manai.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.highliuk.manai.domain.model.AppLanguage
import com.highliuk.manai.domain.model.ReadingMode
import com.highliuk.manai.domain.model.TargetLanguage
import com.highliuk.manai.domain.model.ThemeMode
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class UserPreferencesRepositoryImplTest {

    @get:Rule
    val tmpFolder = TemporaryFolder()

    private val testDispatcher = UnconfinedTestDispatcher()
    private val dataStoreScope = TestScope(testDispatcher + Job())

    private fun createDataStore(): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(
            scope = dataStoreScope,
            produceFile = { tmpFolder.newFile("test_preferences.preferences_pb") }
        )

    private fun createRepository(): UserPreferencesRepositoryImpl =
        UserPreferencesRepositoryImpl(createDataStore())

    @After
    fun tearDown() {
        dataStoreScope.cancel()
    }

    @Test
    fun `gridColumns emits default value 2`() = runTest(testDispatcher) {
        val repository = createRepository()

        val result = repository.gridColumns.first()

        assertEquals(2, result)
    }

    @Test
    fun `setGridColumns persists value`() = runTest(testDispatcher) {
        val repository = createRepository()

        repository.setGridColumns(3)

        assertEquals(3, repository.gridColumns.first())
    }

    @Test
    fun `setGridColumns clamps value below minimum to 2`() = runTest(testDispatcher) {
        val repository = createRepository()

        repository.setGridColumns(1)

        assertEquals(2, repository.gridColumns.first())
    }

    @Test
    fun `setGridColumns clamps value above maximum to 3`() = runTest(testDispatcher) {
        val repository = createRepository()

        repository.setGridColumns(5)

        assertEquals(3, repository.gridColumns.first())
    }

    @Test
    fun `readingMode emits default value LTR`() = runTest(testDispatcher) {
        val repository = createRepository()

        val result = repository.readingMode.first()

        assertEquals(ReadingMode.LTR, result)
    }

    @Test
    fun `setReadingMode persists RTL value`() = runTest(testDispatcher) {
        val repository = createRepository()

        repository.setReadingMode(ReadingMode.RTL)

        assertEquals(ReadingMode.RTL, repository.readingMode.first())
    }

    @Test
    fun `readingMode with invalid stored value defaults to LTR`() = runTest(testDispatcher) {
        val dataStore = createDataStore()
        dataStore.edit { preferences ->
            preferences[stringPreferencesKey("reading_mode")] = "INVALID"
        }
        val repository = UserPreferencesRepositoryImpl(dataStore)

        assertEquals(ReadingMode.LTR, repository.readingMode.first())
    }

    @Test
    fun `themeMode emits default value SYSTEM`() = runTest(testDispatcher) {
        val repository = createRepository()

        val result = repository.themeMode.first()

        assertEquals(ThemeMode.SYSTEM, result)
    }

    @Test
    fun `setThemeMode persists DARK value`() = runTest(testDispatcher) {
        val repository = createRepository()

        repository.setThemeMode(ThemeMode.DARK)

        assertEquals(ThemeMode.DARK, repository.themeMode.first())
    }

    @Test
    fun `setThemeMode persists LIGHT value`() = runTest(testDispatcher) {
        val repository = createRepository()

        repository.setThemeMode(ThemeMode.LIGHT)

        assertEquals(ThemeMode.LIGHT, repository.themeMode.first())
    }

    @Test
    fun `themeMode with invalid stored value defaults to SYSTEM`() = runTest(testDispatcher) {
        val dataStore = createDataStore()
        dataStore.edit { preferences ->
            preferences[stringPreferencesKey("theme_mode")] = "INVALID"
        }
        val repository = UserPreferencesRepositoryImpl(dataStore)

        assertEquals(ThemeMode.SYSTEM, repository.themeMode.first())
    }

    @Test
    fun `setReadingMode persists WEBTOON value`() = runTest(testDispatcher) {
        val repository = createRepository()

        repository.setReadingMode(ReadingMode.WEBTOON)

        assertEquals(ReadingMode.WEBTOON, repository.readingMode.first())
    }

    @Test
    fun `appLanguage emits default value SYSTEM`() = runTest(testDispatcher) {
        val repository = createRepository()

        val result = repository.appLanguage.first()

        assertEquals(AppLanguage.SYSTEM, result)
    }

    @Test
    fun `setAppLanguage persists ITALIAN value`() = runTest(testDispatcher) {
        val repository = createRepository()

        repository.setAppLanguage(AppLanguage.ITALIAN)

        assertEquals(AppLanguage.ITALIAN, repository.appLanguage.first())
    }

    @Test
    fun `appLanguage with invalid stored value defaults to SYSTEM`() = runTest(testDispatcher) {
        val dataStore = createDataStore()
        dataStore.edit { preferences ->
            preferences[stringPreferencesKey("app_language")] = "INVALID"
        }
        val repository = UserPreferencesRepositoryImpl(dataStore)

        assertEquals(AppLanguage.SYSTEM, repository.appLanguage.first())
    }

    @Test
    fun `tapToNavigate emits default value false`() = runTest(testDispatcher) {
        val repository = createRepository()

        val result = repository.tapToNavigate.first()

        assertEquals(false, result)
    }

    @Test
    fun `setTapToNavigate persists true`() = runTest(testDispatcher) {
        val repository = createRepository()

        repository.setTapToNavigate(true)

        assertEquals(true, repository.tapToNavigate.first())
    }

    @Test
    fun `setTapToNavigate persists false after true`() = runTest(testDispatcher) {
        val repository = createRepository()

        repository.setTapToNavigate(true)
        repository.setTapToNavigate(false)

        assertEquals(false, repository.tapToNavigate.first())
    }

    @Test
    fun `tapToNavigatePortrait emits default value false`() = runTest(testDispatcher) {
        val repository = createRepository()

        val result = repository.tapToNavigatePortrait.first()

        assertEquals(false, result)
    }

    @Test
    fun `setTapToNavigatePortrait persists true`() = runTest(testDispatcher) {
        val repository = createRepository()

        repository.setTapToNavigatePortrait(true)

        assertEquals(true, repository.tapToNavigatePortrait.first())
    }

    @Test
    fun `setTapToNavigatePortrait persists false after true`() = runTest(testDispatcher) {
        val repository = createRepository()

        repository.setTapToNavigatePortrait(true)
        repository.setTapToNavigatePortrait(false)

        assertEquals(false, repository.tapToNavigatePortrait.first())
    }

    @Test
    fun `tapToNavigateLandscape emits default value true`() = runTest(testDispatcher) {
        val repository = createRepository()

        val result = repository.tapToNavigateLandscape.first()

        assertEquals(true, result)
    }

    @Test
    fun `setTapToNavigateLandscape persists false`() = runTest(testDispatcher) {
        val repository = createRepository()

        repository.setTapToNavigateLandscape(false)

        assertEquals(false, repository.tapToNavigateLandscape.first())
    }

    @Test
    fun `setTapToNavigateLandscape persists true after false`() = runTest(testDispatcher) {
        val repository = createRepository()

        repository.setTapToNavigateLandscape(false)
        repository.setTapToNavigateLandscape(true)

        assertEquals(true, repository.tapToNavigateLandscape.first())
    }

    @Test
    fun `tapToNavigatePortrait falls back to old tapToNavigate when not set`() = runTest(testDispatcher) {
        val dataStore = createDataStore()
        dataStore.edit { preferences ->
            preferences[booleanPreferencesKey("tap_to_navigate")] = true
        }
        val repository = UserPreferencesRepositoryImpl(dataStore)

        assertEquals(true, repository.tapToNavigatePortrait.first())
    }

    @Test
    fun `tapToNavigateLandscape falls back to old tapToNavigate when not set`() = runTest(testDispatcher) {
        val dataStore = createDataStore()
        dataStore.edit { preferences ->
            preferences[booleanPreferencesKey("tap_to_navigate")] = false
        }
        val repository = UserPreferencesRepositoryImpl(dataStore)

        assertEquals(false, repository.tapToNavigateLandscape.first())
    }

    @Test
    fun `gridColumnsLandscape emits default value 5`() = runTest(testDispatcher) {
        val repository = createRepository()

        val result = repository.gridColumnsLandscape.first()

        assertEquals(5, result)
    }

    @Test
    fun `setGridColumnsLandscape persists value`() = runTest(testDispatcher) {
        val repository = createRepository()

        repository.setGridColumnsLandscape(6)

        assertEquals(6, repository.gridColumnsLandscape.first())
    }

    @Test
    fun `setGridColumnsLandscape clamps value below minimum to 4`() = runTest(testDispatcher) {
        val repository = createRepository()

        repository.setGridColumnsLandscape(2)

        assertEquals(4, repository.gridColumnsLandscape.first())
    }

    @Test
    fun `setGridColumnsLandscape clamps value above maximum to 6`() = runTest(testDispatcher) {
        val repository = createRepository()

        repository.setGridColumnsLandscape(10)

        assertEquals(6, repository.gridColumnsLandscape.first())
    }

    @Test
    fun `ocrFontScale emits default value 1_5`() = runTest(testDispatcher) {
        val repository = createRepository()

        val result = repository.ocrFontScale.first()

        assertEquals(1.5f, result, 1e-4f)
    }

    @Test
    fun `setOcrFontScale persists value`() = runTest(testDispatcher) {
        val repository = createRepository()

        repository.setOcrFontScale(2.0f)

        assertEquals(2.0f, repository.ocrFontScale.first(), 1e-4f)
    }

    @Test
    fun `setOcrFontScale clamps value below minimum to 1_0`() = runTest(testDispatcher) {
        val repository = createRepository()

        repository.setOcrFontScale(0.5f)

        assertEquals(1.0f, repository.ocrFontScale.first(), 1e-4f)
    }

    @Test
    fun `setOcrFontScale clamps value above maximum to 3_0`() = runTest(testDispatcher) {
        val repository = createRepository()

        repository.setOcrFontScale(5.0f)

        assertEquals(3.0f, repository.ocrFontScale.first(), 1e-4f)
    }

    @Test
    fun `translationTargetLang emits default value EN`() = runTest(testDispatcher) {
        val repository = createRepository()

        val result = repository.translationTargetLang.first()

        assertEquals(TargetLanguage.EN, result)
    }

    @Test
    fun `setTranslationTargetLang persists IT value`() = runTest(testDispatcher) {
        val repository = createRepository()

        repository.setTranslationTargetLang(TargetLanguage.IT)

        assertEquals(TargetLanguage.IT, repository.translationTargetLang.first())
    }

    @Test
    fun `translationTargetLang with invalid stored value defaults to EN`() = runTest(testDispatcher) {
        val dataStore = createDataStore()
        dataStore.edit { preferences ->
            preferences[stringPreferencesKey("translation_target_lang")] = "INVALID"
        }
        val repository = UserPreferencesRepositoryImpl(dataStore)

        assertEquals(TargetLanguage.EN, repository.translationTargetLang.first())
    }

    @Test
    fun `llmBaseUrl emits Groq default`() = runTest(testDispatcher) {
        val repository = createRepository()

        assertEquals("https://api.groq.com/openai/v1", repository.llmBaseUrl.first())
    }

    @Test
    fun `setLlmBaseUrl persists value`() = runTest(testDispatcher) {
        val repository = createRepository()

        repository.setLlmBaseUrl("https://api.openai.com/v1")

        assertEquals("https://api.openai.com/v1", repository.llmBaseUrl.first())
    }

    @Test
    fun `llmModel emits default model`() = runTest(testDispatcher) {
        val repository = createRepository()

        assertEquals("llama-3.3-70b-versatile", repository.llmModel.first())
    }

    @Test
    fun `setLlmModel persists value`() = runTest(testDispatcher) {
        val repository = createRepository()

        repository.setLlmModel("qwen-2.5-72b")

        assertEquals("qwen-2.5-72b", repository.llmModel.first())
    }

    @Test
    fun `promptDefaultsSeeded emits default false`() = runTest(testDispatcher) {
        val repository = createRepository()

        assertEquals(false, repository.promptDefaultsSeeded.first())
    }

    @Test
    fun `setPromptDefaultsSeeded persists true`() = runTest(testDispatcher) {
        val repository = createRepository()

        repository.setPromptDefaultsSeeded()

        assertEquals(true, repository.promptDefaultsSeeded.first())
    }
}
