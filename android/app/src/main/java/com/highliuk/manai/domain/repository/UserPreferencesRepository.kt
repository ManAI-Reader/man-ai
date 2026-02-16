package com.highliuk.manai.domain.repository

import com.highliuk.manai.domain.model.ReadingMode
import kotlinx.coroutines.flow.Flow

interface UserPreferencesRepository {
    val gridColumns: Flow<Int>
    suspend fun setGridColumns(columns: Int)

    val readingMode: Flow<ReadingMode>
    suspend fun setReadingMode(mode: ReadingMode)
}
