package com.highliuk.manai.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.highliuk.manai.domain.model.AppLanguage
import com.highliuk.manai.domain.model.ReadingMode
import com.highliuk.manai.domain.model.ThemeMode
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

        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DEFAULT_THEME_MODE = ThemeMode.SYSTEM

        val APP_LANGUAGE = stringPreferencesKey("app_language")
        val DEFAULT_APP_LANGUAGE = AppLanguage.SYSTEM

        val GRID_COLUMNS_LANDSCAPE = intPreferencesKey("grid_columns_landscape")
        const val DEFAULT_GRID_COLUMNS_LANDSCAPE = 5
        const val MIN_GRID_COLUMNS_LANDSCAPE = 4
        const val MAX_GRID_COLUMNS_LANDSCAPE = 6

        val OCR_FONT_SCALE = floatPreferencesKey("ocr_font_scale")
        const val DEFAULT_OCR_FONT_SCALE = 1.5f
        const val MIN_OCR_FONT_SCALE = 1.0f
        const val MAX_OCR_FONT_SCALE = 3.0f

        val TAP_TO_NAVIGATE = booleanPreferencesKey("tap_to_navigate")
        const val DEFAULT_TAP_TO_NAVIGATE = false
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

    override val gridColumnsLandscape: Flow<Int> = dataStore.data.map { preferences ->
        preferences[GRID_COLUMNS_LANDSCAPE] ?: DEFAULT_GRID_COLUMNS_LANDSCAPE
    }

    override suspend fun setGridColumnsLandscape(columns: Int) {
        val clamped = columns.coerceIn(MIN_GRID_COLUMNS_LANDSCAPE, MAX_GRID_COLUMNS_LANDSCAPE)
        dataStore.edit { preferences ->
            preferences[GRID_COLUMNS_LANDSCAPE] = clamped
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

    override val themeMode: Flow<ThemeMode> = dataStore.data.map { preferences ->
        val stored = preferences[THEME_MODE]
        if (stored != null) {
            try {
                ThemeMode.valueOf(stored)
            } catch (_: IllegalArgumentException) {
                DEFAULT_THEME_MODE
            }
        } else {
            DEFAULT_THEME_MODE
        }
    }

    override suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { preferences ->
            preferences[THEME_MODE] = mode.name
        }
    }

    override val appLanguage: Flow<AppLanguage> = dataStore.data.map { preferences ->
        val stored = preferences[APP_LANGUAGE]
        if (stored != null) {
            try {
                AppLanguage.valueOf(stored)
            } catch (_: IllegalArgumentException) {
                DEFAULT_APP_LANGUAGE
            }
        } else {
            DEFAULT_APP_LANGUAGE
        }
    }

    override suspend fun setAppLanguage(language: AppLanguage) {
        dataStore.edit { preferences ->
            preferences[APP_LANGUAGE] = language.name
        }
    }

    override val ocrFontScale: Flow<Float> = dataStore.data.map { preferences ->
        preferences[OCR_FONT_SCALE] ?: DEFAULT_OCR_FONT_SCALE
    }

    override suspend fun setOcrFontScale(scale: Float) {
        val clamped = scale.coerceIn(MIN_OCR_FONT_SCALE, MAX_OCR_FONT_SCALE)
        dataStore.edit { preferences ->
            preferences[OCR_FONT_SCALE] = clamped
        }
    }

    override val tapToNavigate: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[TAP_TO_NAVIGATE] ?: DEFAULT_TAP_TO_NAVIGATE
    }

    override suspend fun setTapToNavigate(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[TAP_TO_NAVIGATE] = enabled
        }
    }
}
