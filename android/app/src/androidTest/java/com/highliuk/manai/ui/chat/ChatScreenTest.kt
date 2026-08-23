package com.highliuk.manai.ui.chat

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.test.espresso.Espresso
import com.highliuk.manai.domain.model.ChatMessage
import com.highliuk.manai.domain.model.ChatRole
import com.highliuk.manai.domain.model.Conversation
import com.highliuk.manai.domain.model.FuriganaPart
import com.highliuk.manai.domain.model.FuriganaToken
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
        val resolveFurigana: FuriganaResolver? = null,
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
                resolveFurigana = state.resolveFurigana,
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
    fun ttfbSpinnerVisibleWhileGeneratingBeforeFirstDelta() {
        setChatContent(ChatState(conversation, streamingText = null, isGenerating = true))

        composeTestRule.onNodeWithTag("chat_streaming").assertIsDisplayed()
        composeTestRule.onNodeWithTag("chat_ttfb_spinner").assertIsDisplayed()
    }

    @Test
    fun ttfbSpinnerVisibleWithEmptyStreamingText() {
        setChatContent(ChatState(conversation, streamingText = "", isGenerating = true))

        composeTestRule.onNodeWithTag("chat_ttfb_spinner").assertIsDisplayed()
    }

    @Test
    fun ttfbSpinnerGoneOnceStreamingTextArrives() {
        setChatContent(
            ChatState(conversation, streamingText = "Hello there", isGenerating = true)
        )

        composeTestRule.onNodeWithTag("chat_ttfb_spinner").assertDoesNotExist()
        composeTestRule.onNodeWithText("Hello there").assertIsDisplayed()
    }

    @Test
    fun assistantMarkdownRendersBoldAndListWithoutRawMarkers() {
        setChatContent(
            ChatState(
                conversation = conversation,
                messages = listOf(
                    ChatMessage(
                        id = 1L,
                        conversationId = 1L,
                        role = ChatRole.ASSISTANT,
                        content = "**Meaning** of the word\n\n- first item\n- second item",
                        timestamp = 0L,
                    ),
                ),
            ),
        )

        composeTestRule.onNodeWithText("Meaning of the word").assertIsDisplayed()
        composeTestRule.onNodeWithText("first item").assertIsDisplayed()
        composeTestRule.onNodeWithText("second item").assertIsDisplayed()
        composeTestRule.onNodeWithText("**Meaning** of the word").assertDoesNotExist()
    }

    @Test
    fun assistantJapaneseTextRequestsFuriganaFromInjectedResolver() {
        val requested = mutableListOf<String>()
        val tokens = listOf(
            FuriganaToken(
                surface = "漢字",
                reading = "カンジ",
                parts = listOf(FuriganaPart.kanji("漢字", "かんじ")),
            ),
        )
        setChatContent(
            ChatState(
                conversation = conversation,
                messages = listOf(
                    ChatMessage(
                        id = 1L,
                        conversationId = 1L,
                        role = ChatRole.ASSISTANT,
                        content = "漢字",
                        timestamp = 0L,
                    ),
                ),
                resolveFurigana = { run ->
                    requested.add(run)
                    tokens
                },
            ),
        )

        composeTestRule.waitUntil(timeoutMillis = 5_000) { requested.contains("漢字") }
        composeTestRule.onNodeWithText("漢字").assertIsDisplayed()
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

    private fun inputHeight(): Dp =
        composeTestRule.onNodeWithTag("chat_input").getUnclippedBoundsInRoot().height

    @Test
    fun inputGrowsWithMultilineTextAndCapsAtFiveLines() {
        setChatContent(ChatState(conversation))

        val singleLineHeight = inputHeight()

        composeTestRule.onNodeWithTag("chat_input").performTextInput("1\n2\n3")
        val threeLineHeight = inputHeight()
        assertTrue(threeLineHeight > singleLineHeight)

        composeTestRule.onNodeWithTag("chat_input").performTextInput("\n4\n5")
        val fiveLineHeight = inputHeight()
        assertTrue(fiveLineHeight > threeLineHeight)

        composeTestRule.onNodeWithTag("chat_input").performTextInput("\n6\n7")
        val sevenLineHeight = inputHeight()
        assertEquals(fiveLineHeight.value, sevenLineHeight.value, 0.5f)
    }

    @Test
    fun messageListStaysVisibleWhileComposingMultilineInput() {
        setChatContent(
            ChatState(
                conversation = conversation,
                messages = listOf(
                    ChatMessage(
                        id = 1L,
                        conversationId = 1L,
                        role = ChatRole.USER,
                        content = "Earlier message",
                        timestamp = 0L,
                    ),
                ),
            ),
        )

        composeTestRule.onNodeWithTag("chat_input").performTextInput("1\n2\n3\n4\n5")
        // The compose test host activity is not edge-to-edge, so an open IME
        // gets compensated twice (host resize + imePadding) and collapses the
        // list in a way that cannot happen in the real app. Dismiss the
        // keyboard and assert the layout with the full-grown input.
        Espresso.closeSoftKeyboard()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Earlier message").assertIsDisplayed()
    }

    @Test
    fun sendButtonAlignsToBottomOfInputBarWithMultilineText() {
        setChatContent(ChatState(conversation))

        composeTestRule.onNodeWithTag("chat_input").performTextInput("1\n2\n3\n4\n5")

        val inputBounds = composeTestRule.onNodeWithTag("chat_input")
            .getUnclippedBoundsInRoot()
        val sendBounds = composeTestRule.onNodeWithContentDescription("Send")
            .getUnclippedBoundsInRoot()

        // Bottom-aligned: the send button bottom must sit at the input bar
        // bottom (small tolerance for rounding), not centered ~46dp above it.
        assertTrue(inputBounds.bottom - sendBounds.bottom <= 8.dp)
    }

    @Test
    fun backButtonInvokesCallback() {
        var backed = false
        setChatContent(ChatState(conversation), onBack = { backed = true })

        composeTestRule.onNodeWithContentDescription("Back").performClick()

        assertTrue(backed)
    }
}
