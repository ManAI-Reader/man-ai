package com.highliuk.manai.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.highliuk.manai.domain.model.ReadingMode
import com.highliuk.manai.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class UserPreferencesRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : UserPreferencesRepository {

    private companion object {
        val GRID_COLUMNS = intPreferencesKey("grid_columns")
        const val DEFAULT_GRID_COLUMNS = 2
        const val MIN_GRID_COLUMNS = 2
        const val MAX_GRID_COLUMNS = 3

        val READING_MODE = stringPreferencesKey("reading_mode")
        val DEFAULT_READING_MODE = ReadingMode.LTR
    }

    override val gridColumns: Flow<Int> = dataStore.data.map { preferences ->
        preferences[GRID_COLUMNS] ?: DEFAULT_GRID_COLUMNS
    }

    override suspend fun setGridColumns(columns: Int) {
        val clamped = columns.coerceIn(MIN_GRID_COLUMNS, MAX_GRID_COLUMNS)
        dataStore.edit { preferences ->
            preferences[GRID_COLUMNS] = clamped
        }
    }

    override val readingMode: Flow<ReadingMode> = dataStore.data.map { preferences ->
        val stored = preferences[READING_MODE]
        if (stored != null) {
            try {
                ReadingMode.valueOf(stored)
            } catch (_: IllegalArgumentException) {
                DEFAULT_READING_MODE
            }
        } else {
            DEFAULT_READING_MODE
        }
    }

    override suspend fun setReadingMode(mode: ReadingMode) {
        dataStore.edit { preferences ->
            preferences[READING_MODE] = mode.name
        }
    }
}
