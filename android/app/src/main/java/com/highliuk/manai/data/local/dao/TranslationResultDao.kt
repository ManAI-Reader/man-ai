package com.highliuk.manai.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.highliuk.manai.data.local.entity.TranslationResultEntity

@Dao
interface TranslationResultDao {
    @Query(
        "SELECT * FROM translation_result " +
            "WHERE mangaId = :mangaId AND pageIndex = :pageIndex " +
            "AND regionIndex = :regionIndex AND provider = :provider"
    )
    suspend fun get(
        mangaId: Long,
        pageIndex: Int,
        regionIndex: Int,
        provider: String,
    ): TranslationResultEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: TranslationResultEntity)
}
