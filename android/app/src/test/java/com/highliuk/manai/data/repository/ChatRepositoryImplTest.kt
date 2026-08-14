package com.highliuk.manai.data.repository

import com.highliuk.manai.data.local.dao.ChatMessageDao
import com.highliuk.manai.data.local.dao.ConversationDao
import com.highliuk.manai.data.local.entity.ChatMessageEntity
import com.highliuk.manai.data.local.entity.ConversationEntity
import com.highliuk.manai.domain.model.ChatMessage
import com.highliuk.manai.domain.model.ChatRole
import com.highliuk.manai.domain.model.Conversation
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ChatRepositoryImplTest {

    private val conversationDao = mockk<ConversationDao>(relaxed = true)
    private val chatMessageDao = mockk<ChatMessageDao>(relaxed = true)
    private val repository = ChatRepositoryImpl(
        conversationDao = conversationDao,
        chatMessageDao = chatMessageDao,
        clock = { FIXED_NOW },
    )

    @Test
    fun `observeConversations maps entities to domain models`() = runTest {
        val entity = ConversationEntity(
            id = 7L,
            title = "Grammar chat",
            mangaId = 3L,
            pageIndex = 12,
            regionIndex = 2,
            createdAt = 100L,
            updatedAt = 200L,
        )
        every { conversationDao.observeAll() } returns flowOf(listOf(entity))

        val result = repository.observeConversations().first()

        assertEquals(
            listOf(
                Conversation(
                    id = 7L,
                    title = "Grammar chat",
                    mangaId = 3L,
                    pageIndex = 12,
                    regionIndex = 2,
                    createdAt = 100L,
                    updatedAt = 200L,
                )
            ),
            result
        )
    }

    @Test
    fun `observeConversation maps entity and emits null when missing`() = runTest {
        every { conversationDao.observeById(9L) } returns flowOf(null)

        assertNull(repository.observeConversation(9L).first())
    }

    @Test
    fun `observeMessages maps roles to domain enum`() = runTest {
        val entities = listOf(
            ChatMessageEntity(1L, 5L, "user", "hi", 10L),
            ChatMessageEntity(2L, 5L, "assistant", "hello", 20L),
        )
        every { chatMessageDao.observeByConversation(5L) } returns flowOf(entities)

        val result = repository.observeMessages(5L).first()

        assertEquals(
            listOf(
                ChatMessage(1L, 5L, ChatRole.USER, "hi", 10L),
                ChatMessage(2L, 5L, ChatRole.ASSISTANT, "hello", 20L),
            ),
            result
        )
    }

    @Test
    fun `unknown role string maps defensively to ASSISTANT`() = runTest {
        val entities = listOf(ChatMessageEntity(1L, 5L, "tool", "output", 10L))
        coEvery { chatMessageDao.getByConversation(5L) } returns entities

        val result = repository.getMessages(5L)

        assertEquals(ChatRole.ASSISTANT, result.single().role)
    }

    @Test
    fun `createConversation returns generated id with matching timestamps`() = runTest {
        val entitySlot = slot<ConversationEntity>()
        coEvery { conversationDao.insert(capture(entitySlot)) } returns 42L

        val id = repository.createConversation(
            title = "New chat",
            mangaId = 1L,
            pageIndex = 0,
            regionIndex = null,
        )

        assertEquals(42L, id)
        assertEquals("New chat", entitySlot.captured.title)
        assertEquals(FIXED_NOW, entitySlot.captured.createdAt)
        assertEquals(FIXED_NOW, entitySlot.captured.updatedAt)
    }

    @Test
    fun `appendMessage inserts entity and touches conversation`() = runTest {
        val entitySlot = slot<ChatMessageEntity>()
        coEvery { chatMessageDao.insert(capture(entitySlot)) } returns 1L

        repository.appendMessage(5L, ChatRole.USER, "hello")

        assertEquals(5L, entitySlot.captured.conversationId)
        assertEquals("user", entitySlot.captured.role)
        assertEquals("hello", entitySlot.captured.content)
        assertEquals(FIXED_NOW, entitySlot.captured.timestamp)
        coVerify { conversationDao.touch(5L, FIXED_NOW) }
    }

    @Test
    fun `deleteConversation delegates to dao`() = runTest {
        repository.deleteConversation(7L)

        coVerify { conversationDao.delete(7L) }
    }

    private companion object {
        const val FIXED_NOW = 1_723_600_000_000L
    }
}
