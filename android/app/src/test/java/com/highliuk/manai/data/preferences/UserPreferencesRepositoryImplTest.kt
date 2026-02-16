package com.highliuk.manai.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.highliuk.manai.domain.model.ReadingMode
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
}
