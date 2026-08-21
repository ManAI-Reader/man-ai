package com.highliuk.manai.data.repository

import com.highliuk.manai.data.local.dao.ChatMessageDao
import com.highliuk.manai.data.local.dao.ConversationDao
import com.highliuk.manai.data.local.entity.ChatMessageEntity
import com.highliuk.manai.data.local.entity.ConversationEntity
import com.highliuk.manai.domain.model.ChatMessage
import com.highliuk.manai.domain.model.ChatRole
import com.highliuk.manai.domain.model.Conversation
import com.highliuk.manai.domain.model.ReasoningLevel
import com.highliuk.manai.domain.repository.ChatRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ChatRepositoryImpl(
    private val conversationDao: ConversationDao,
    private val chatMessageDao: ChatMessageDao,
    private val clock: () -> Long = System::currentTimeMillis,
) : ChatRepository {

    override fun observeConversations(): Flow<List<Conversation>> =
        conversationDao.observeAll().map { entities -> entities.map { it.toDomain() } }

    override fun observeConversation(id: Long): Flow<Conversation?> =
        conversationDao.observeById(id).map { it?.toDomain() }

    override fun observeMessages(conversationId: Long): Flow<List<ChatMessage>> =
        chatMessageDao.observeByConversation(conversationId)
            .map { entities -> entities.map { it.toDomain() } }

    override suspend fun getMessages(conversationId: Long): List<ChatMessage> =
        chatMessageDao.getByConversation(conversationId).map { it.toDomain() }

    override suspend fun createConversation(
        title: String,
        mangaId: Long?,
        pageIndex: Int?,
        regionIndex: Int?,
        reasoningLevel: ReasoningLevel,
    ): Long {
        val now = clock()
        return conversationDao.insert(
            ConversationEntity(
                title = title,
                mangaId = mangaId,
                pageIndex = pageIndex,
                regionIndex = regionIndex,
                createdAt = now,
                updatedAt = now,
                reasoningLevel = reasoningLevel.name,
            )
        )
    }

    override suspend fun appendMessage(conversationId: Long, role: ChatRole, content: String) {
        val now = clock()
        chatMessageDao.insert(
            ChatMessageEntity(
                conversationId = conversationId,
                role = role.toStorage(),
                content = content,
                timestamp = now,
            )
        )
        conversationDao.touch(conversationId, now)
    }

    override suspend fun deleteConversation(id: Long) {
        conversationDao.delete(id)
    }

    private fun ConversationEntity.toDomain(): Conversation = Conversation(
        id = id,
        title = title,
        mangaId = mangaId,
        pageIndex = pageIndex,
        regionIndex = regionIndex,
        createdAt = createdAt,
        updatedAt = updatedAt,
        reasoningLevel = ReasoningLevel.valueOfOrDefault(reasoningLevel),
    )

    private fun ChatMessageEntity.toDomain(): ChatMessage = ChatMessage(
        id = id,
        conversationId = conversationId,
        role = role.toChatRole(),
        content = content,
        timestamp = timestamp,
    )

    private fun ChatRole.toStorage(): String = when (this) {
        ChatRole.USER -> ROLE_USER
        ChatRole.ASSISTANT -> ROLE_ASSISTANT
    }

    private fun String.toChatRole(): ChatRole = when (this) {
        ROLE_USER -> ChatRole.USER
        else -> ChatRole.ASSISTANT
    }

    private companion object {
        const val ROLE_USER = "user"
        const val ROLE_ASSISTANT = "assistant"
    }
}
