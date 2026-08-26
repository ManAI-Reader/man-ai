package com.highliuk.manai.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.highliuk.manai.data.local.entity.PromptTemplateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PromptTemplateDao {
    @Query("SELECT * FROM prompt_template ORDER BY sortOrder ASC, id ASC")
    fun observeAll(): Flow<List<PromptTemplateEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: PromptTemplateEntity): Long

    @Query("DELETE FROM prompt_template WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT COUNT(*) FROM prompt_template")
    suspend fun count(): Int
}
