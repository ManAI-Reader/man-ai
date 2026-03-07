package com.highliuk.manai.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.highliuk.manai.domain.model.AppLanguage
import com.highliuk.manai.domain.model.ReadingMode
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
}
