package com.highliuk.manai.domain.repository

import com.highliuk.manai.domain.model.ChatMessage
import com.highliuk.manai.domain.model.ChatRole
import com.highliuk.manai.domain.model.Conversation
import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    fun observeConversations(): Flow<List<Conversation>>
    fun observeConversation(id: Long): Flow<Conversation?>
    fun observeMessages(conversationId: Long): Flow<List<ChatMessage>>
    suspend fun getMessages(conversationId: Long): List<ChatMessage>
    suspend fun createConversation(
        title: String,
        mangaId: Long?,
        pageIndex: Int?,
        regionIndex: Int?,
    ): Long
    suspend fun appendMessage(conversationId: Long, role: ChatRole, content: String)
    suspend fun deleteConversation(id: Long)
}
