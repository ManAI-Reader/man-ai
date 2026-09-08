package com.highliuk.manai.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.highliuk.manai.data.local.entity.ChatMessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatMessageDao {
    @Query("SELECT * FROM chat_message WHERE conversationId = :conversationId ORDER BY id ASC")
    fun observeByConversation(conversationId: Long): Flow<List<ChatMessageEntity>>

    @Query("SELECT * FROM chat_message WHERE conversationId = :conversationId ORDER BY id ASC")
    suspend fun getByConversation(conversationId: Long): List<ChatMessageEntity>

    @Insert
    suspend fun insert(entity: ChatMessageEntity): Long
}
