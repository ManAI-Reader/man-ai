package com.highliuk.manai.domain.repository

import com.highliuk.manai.domain.model.AppLanguage
import com.highliuk.manai.domain.model.ReadingMode
import com.highliuk.manai.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow

interface UserPreferencesRepository {
    val gridColumns: Flow<Int>
    suspend fun setGridColumns(columns: Int)

    val readingMode: Flow<ReadingMode>
    suspend fun setReadingMode(mode: ReadingMode)

    val themeMode: Flow<ThemeMode>
    suspend fun setThemeMode(mode: ThemeMode)

    val appLanguage: Flow<AppLanguage>
    suspend fun setAppLanguage(language: AppLanguage)

    val tapToNavigate: Flow<Boolean>
    suspend fun setTapToNavigate(enabled: Boolean)
}
