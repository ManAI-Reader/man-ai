package com.highliuk.manai.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.highliuk.manai.data.local.entity.MemoryEntryEntity

@Dao
interface MemoryEntryDao {
    @Query("SELECT title FROM memory_entry ORDER BY title ASC")
    suspend fun listTitles(): List<String>

    @Query("SELECT * FROM memory_entry WHERE title = :title")
    suspend fun get(title: String): MemoryEntryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: MemoryEntryEntity)
}
