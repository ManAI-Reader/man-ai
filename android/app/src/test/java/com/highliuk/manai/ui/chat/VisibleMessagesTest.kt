package com.highliuk.manai.ui.chat

import com.highliuk.manai.domain.model.ChatMessage
import com.highliuk.manai.domain.model.ChatRole
import com.highliuk.manai.domain.model.Conversation
import org.junit.Assert.assertEquals
import org.junit.Test

class VisibleMessagesTest {

    private val templateLaunched = Conversation(
        id = 1L,
        title = "食べる",
        mangaId = 5L,
        pageIndex = 12,
        regionIndex = 3,
        createdAt = 0L,
        updatedAt = 0L,
    )

    private val withoutSource = Conversation(
        id = 2L,
        title = "Chat",
        createdAt = 0L,
        updatedAt = 0L,
    )

    private fun message(id: Long, role: ChatRole, content: String) = ChatMessage(
        id = id,
        conversationId = 1L,
        role = role,
        content = content,
        timestamp = id,
    )

    @Test
    fun hidesRenderedPromptInTemplateLaunchedConversation() {
        val prompt = message(1L, ChatRole.USER, "You are my tutor… huge rendered prompt")
        val reply = message(2L, ChatRole.ASSISTANT, "Sure!")
        val followUp = message(3L, ChatRole.USER, "And this?")

        assertEquals(
            listOf(reply, followUp),
            visibleMessages(templateLaunched, listOf(prompt, reply, followUp)),
        )
    }

    @Test
    fun keepsFirstUserMessageWhenConversationHasNoSourceRegion() {
        val first = message(1L, ChatRole.USER, "Free question")

        assertEquals(
            listOf(first),
            visibleMessages(withoutSource, listOf(first)),
        )
    }

    @Test
    fun keepsFirstMessageWhenItIsFromTheAssistant() {
        val reply = message(1L, ChatRole.ASSISTANT, "Reply")

        assertEquals(
            listOf(reply),
            visibleMessages(templateLaunched, listOf(reply)),
        )
    }

    @Test
    fun keepsAllMessagesWhileConversationIsStillLoading() {
        val first = message(1L, ChatRole.USER, "prompt")

        assertEquals(
            listOf(first),
            visibleMessages(null, listOf(first)),
        )
    }

    @Test
    fun emptyListStaysEmpty() {
        assertEquals(
            emptyList<ChatMessage>(),
            visibleMessages(templateLaunched, emptyList()),
        )
    }
}
