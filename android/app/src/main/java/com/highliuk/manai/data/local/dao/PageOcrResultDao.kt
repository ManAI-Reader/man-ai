package com.highliuk.manai.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.highliuk.manai.data.local.entity.PageOcrResultEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PageOcrResultDao {
    @Query(
        "SELECT * FROM page_ocr_result " +
            "WHERE mangaId = :mangaId AND pageIndex = :pageIndex " +
            "ORDER BY regionIndex"
    )
    fun getByPage(mangaId: Long, pageIndex: Int): Flow<List<PageOcrResultEntity>>

    @Query(
        "SELECT * FROM page_ocr_result " +
            "WHERE mangaId = :mangaId AND pageIndex = :pageIndex " +
            "ORDER BY regionIndex"
    )
    suspend fun getByPageOnce(mangaId: Long, pageIndex: Int): List<PageOcrResultEntity>

    @Query(
        "SELECT COUNT(*) > 0 FROM page_ocr_result " +
            "WHERE mangaId = :mangaId AND pageIndex = :pageIndex"
    )
    suspend fun hasDetectionResults(mangaId: Long, pageIndex: Int): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<PageOcrResultEntity>)

    @Query(
        "UPDATE page_ocr_result SET ocrText = :text " +
            "WHERE mangaId = :mangaId AND pageIndex = :pageIndex AND regionIndex = :regionIndex"
    )
    suspend fun updateOcrText(mangaId: Long, pageIndex: Int, regionIndex: Int, text: String)

    @Query("DELETE FROM page_ocr_result WHERE mangaId = :mangaId")
    suspend fun deleteByManga(mangaId: Long)
}
