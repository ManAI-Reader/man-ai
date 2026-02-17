package com.highliuk.manai.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.highliuk.manai.data.local.entity.MangaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MangaDao {
    @Query("SELECT * FROM manga")
    fun getAll(): Flow<List<MangaEntity>>

    @Query("SELECT * FROM manga WHERE id = :id")
    fun getById(id: Long): Flow<MangaEntity?>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entity: MangaEntity): Long

    @Query("SELECT * FROM manga WHERE uri = :uri LIMIT 1")
    suspend fun getByUri(uri: String): MangaEntity?

    @Query("UPDATE manga SET lastReadPage = :page WHERE id = :id")
    suspend fun updateLastReadPage(id: Long, page: Int)
}
