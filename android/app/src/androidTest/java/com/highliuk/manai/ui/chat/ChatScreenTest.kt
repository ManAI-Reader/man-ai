package com.highliuk.manai.ui.chat

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.highliuk.manai.domain.model.ChatMessage
import com.highliuk.manai.domain.model.ChatRole
import com.highliuk.manai.domain.model.Conversation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ChatScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val conversation = Conversation(
        id = 1L,
        title = "Grammar question",
        createdAt = 0L,
        updatedAt = 0L,
    )

    private val conversationWithSource = conversation.copy(
        mangaId = 42L,
        pageIndex = 3,
        regionIndex = 0,
    )

    private data class ChatState(
        val conversation: Conversation?,
        val messages: List<ChatMessage> = emptyList(),
        val streamingText: String? = null,
        val isGenerating: Boolean = false,
        val error: String? = null,
    )

    private fun setChatContent(
        state: ChatState,
        onSend: (String) -> Unit = {},
        onRetry: () -> Unit = {},
        onJumpToSource: () -> Unit = {},
        onBack: () -> Unit = {},
    ) {
        composeTestRule.setContent {
            ChatScreen(
                conversation = state.conversation,
                messages = state.messages,
                streamingText = state.streamingText,
                isGenerating = state.isGenerating,
                error = state.error,
                onSend = onSend,
                onRetry = onRetry,
                onJumpToSource = onJumpToSource,
                onBack = onBack,
            )
        }
    }

    @Test
    fun displaysConversationTitle() {
        setChatContent(ChatState(conversation))

        composeTestRule.onNodeWithText("Grammar question").assertIsDisplayed()
    }

    @Test
    fun rendersUserAndAssistantMessages() {
        setChatContent(
            ChatState(
                conversation = conversation,
                messages = listOf(
                    ChatMessage(
                        id = 1L,
                        conversationId = 1L,
                        role = ChatRole.USER,
                        content = "What does this mean?",
                        timestamp = 0L,
                    ),
                    ChatMessage(
                        id = 2L,
                        conversationId = 1L,
                        role = ChatRole.ASSISTANT,
                        content = "It is a greeting.",
                        timestamp = 1L,
                    ),
                ),
            ),
        )

        composeTestRule.onNodeWithText("What does this mean?").assertIsDisplayed()
        composeTestRule.onNodeWithText("It is a greeting.").assertIsDisplayed()
    }

    @Test
    fun streamingBubbleVisibleWithPartialText() {
        setChatContent(
            ChatState(conversation, streamingText = "Typing an answer", isGenerating = true)
        )

        composeTestRule.onNodeWithTag("chat_streaming").assertIsDisplayed()
        composeTestRule.onNodeWithText("Typing an answer").assertIsDisplayed()
    }

    @Test
    fun streamingBubbleVisibleWithEmptyPartialText() {
        setChatContent(ChatState(conversation, streamingText = "", isGenerating = true))

        composeTestRule.onNodeWithTag("chat_streaming").assertIsDisplayed()
    }

    @Test
    fun streamingBubbleAbsentWhenNotStreaming() {
        setChatContent(ChatState(conversation, streamingText = null))

        composeTestRule.onNodeWithTag("chat_streaming").assertDoesNotExist()
    }

    @Test
    fun typingAndSendInvokesCallbackAndClearsInput() {
        var sent: String? = null
        setChatContent(ChatState(conversation), onSend = { sent = it })

        composeTestRule.onNodeWithTag("chat_input").performTextInput("Hello")
        composeTestRule.onNodeWithContentDescription("Send").performClick()

        assertEquals("Hello", sent)
        composeTestRule.onNodeWithText("Hello").assertDoesNotExist()
    }

    @Test
    fun sendDisabledWhenInputBlank() {
        setChatContent(ChatState(conversation))

        composeTestRule.onNodeWithContentDescription("Send").assertIsNotEnabled()
    }

    @Test
    fun sendDisabledWhileGenerating() {
        setChatContent(ChatState(conversation, isGenerating = true))

        composeTestRule.onNodeWithTag("chat_input").performTextInput("Hello")

        composeTestRule.onNodeWithContentDescription("Send").assertIsNotEnabled()
    }

    @Test
    fun sendEnabledWithInputWhenIdle() {
        setChatContent(ChatState(conversation))

        composeTestRule.onNodeWithTag("chat_input").performTextInput("Hello")

        composeTestRule.onNodeWithContentDescription("Send").assertIsEnabled()
    }

    @Test
    fun errorShowsMessageAndRetryInvokesCallback() {
        var retried = false
        setChatContent(
            ChatState(conversation, error = "Network down"),
            onRetry = { retried = true },
        )

        composeTestRule.onNodeWithText("Network down").assertIsDisplayed()
        composeTestRule.onNodeWithText("Retry").performClick()

        assertTrue(retried)
    }

    @Test
    fun blankErrorFallsBackToGenericMessage() {
        setChatContent(ChatState(conversation, error = " "))

        composeTestRule.onNodeWithText("Something went wrong").assertIsDisplayed()
    }

    @Test
    fun jumpToSourceVisibleAndClickableWhenConversationHasSource() {
        var jumped = false
        setChatContent(
            ChatState(conversationWithSource),
            onJumpToSource = { jumped = true },
        )

        composeTestRule.onNodeWithTag("jump_to_source").assertIsDisplayed().performClick()

        assertTrue(jumped)
    }

    @Test
    fun jumpToSourceHiddenWithoutSource() {
        setChatContent(ChatState(conversation))

        composeTestRule.onNodeWithTag("jump_to_source").assertDoesNotExist()
    }

    @Test
    fun backButtonInvokesCallback() {
        var backed = false
        setChatContent(ChatState(conversation), onBack = { backed = true })

        composeTestRule.onNodeWithContentDescription("Back").performClick()

        assertTrue(backed)
    }
}
