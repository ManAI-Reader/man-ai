package com.highliuk.manai.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.highliuk.manai.data.local.entity.MangaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MangaDao {
    @Query("SELECT * FROM manga ORDER BY addedAt DESC")
    fun getAll(): Flow<List<MangaEntity>>

    @Query("SELECT * FROM manga WHERE id = :id")
    suspend fun getById(id: Long): MangaEntity?

    @Query("SELECT * FROM manga WHERE filePath = :filePath LIMIT 1")
    suspend fun getByFilePath(filePath: String): MangaEntity?

    @Insert
    suspend fun insert(manga: MangaEntity): Long

    @Query("UPDATE manga SET lastReadPage = :page WHERE id = :id")
    suspend fun updateLastReadPage(id: Long, page: Int)

    @Query("DELETE FROM manga WHERE id = :id")
    suspend fun delete(id: Long)
}
