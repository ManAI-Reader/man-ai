package com.highliuk.manai.ui.chat

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.test.espresso.Espresso
import com.highliuk.manai.R
import com.highliuk.manai.domain.model.ChatMessage
import com.highliuk.manai.domain.model.ChatRole
import com.highliuk.manai.domain.model.Conversation
import com.highliuk.manai.domain.model.FuriganaPart
import com.highliuk.manai.domain.model.FuriganaToken
import kotlinx.coroutines.CompletableDeferred
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
        val error: ChatUiError? = null,
        val truncated: Boolean = false,
        val resolveFurigana: FuriganaResolver? = null,
    )

    private data class ChatCallbacks(
        val onSend: (String) -> Unit = {},
        val onRetry: () -> Unit = {},
        val onOpenSourcePage: () -> Unit = {},
        val onDeleteConversation: () -> Unit = {},
        val onBack: () -> Unit = {},
    )

    private fun setChatContent(state: ChatState, callbacks: ChatCallbacks = ChatCallbacks()) {
        composeTestRule.setContent {
            ChatScreen(
                conversation = state.conversation,
                messages = state.messages,
                streamingText = state.streamingText,
                isGenerating = state.isGenerating,
                error = state.error,
                truncated = state.truncated,
                onSend = callbacks.onSend,
                onRetry = callbacks.onRetry,
                onOpenSourcePage = callbacks.onOpenSourcePage,
                onDeleteConversation = callbacks.onDeleteConversation,
                onBack = callbacks.onBack,
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
        setChatContent(ChatState(conversation), ChatCallbacks(onSend = { sent = it }))

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
    fun errorShowsLocalizedMessageAndRetryInvokesCallback() {
        var retried = false
        setChatContent(
            ChatState(conversation, error = ChatUiError(R.string.chat_error_network)),
            ChatCallbacks(onRetry = { retried = true }),
        )

        composeTestRule
            .onNodeWithText("Network error — check your connection")
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("Retry").performClick()

        assertTrue(retried)
    }

    @Test
    fun httpErrorShowsStatusCode() {
        setChatContent(
            ChatState(conversation, error = ChatUiError(R.string.chat_error_http, 502)),
        )

        composeTestRule.onNodeWithText("Request failed (HTTP 502)").assertIsDisplayed()
    }

    @Test
    fun menuShowsOpenSourceEntryWhenConversationHasSource() {
        var opened = false
        setChatContent(
            ChatState(conversationWithSource),
            ChatCallbacks(onOpenSourcePage = { opened = true }),
        )

        composeTestRule.onNodeWithTag("chat_menu").performClick()
        composeTestRule.onNodeWithTag("menu_open_source").assertIsDisplayed().performClick()

        assertTrue(opened)
    }

    @Test
    fun menuHidesOpenSourceEntryWithoutSource() {
        setChatContent(ChatState(conversation))

        composeTestRule.onNodeWithTag("chat_menu").performClick()

        composeTestRule.onNodeWithTag("menu_open_source").assertDoesNotExist()
        composeTestRule.onNodeWithTag("menu_delete").assertIsDisplayed()
    }

    @Test
    fun deleteFlowConfirmsBeforeInvokingCallback() {
        var deleted = false
        setChatContent(
            ChatState(conversation),
            ChatCallbacks(onDeleteConversation = { deleted = true }),
        )

        composeTestRule.onNodeWithTag("chat_menu").performClick()
        composeTestRule.onNodeWithTag("menu_delete").performClick()
        composeTestRule.onNodeWithText("Delete this conversation?").assertIsDisplayed()
        assertEquals(false, deleted)

        composeTestRule.onNodeWithText("Delete").performClick()

        assertTrue(deleted)
    }

    @Test
    fun cancellingDeleteDialogDoesNotInvokeCallback() {
        var deleted = false
        setChatContent(
            ChatState(conversation),
            ChatCallbacks(onDeleteConversation = { deleted = true }),
        )

        composeTestRule.onNodeWithTag("chat_menu").performClick()
        composeTestRule.onNodeWithTag("menu_delete").performClick()
        composeTestRule.onNodeWithText("Cancel").performClick()

        composeTestRule.onNodeWithText("Delete this conversation?").assertDoesNotExist()
        assertEquals(false, deleted)
    }

    @Test
    fun renderedPromptHiddenInTemplateLaunchedConversation() {
        setChatContent(
            ChatState(
                conversation = conversationWithSource,
                messages = listOf(
                    ChatMessage(
                        id = 1L,
                        conversationId = 1L,
                        role = ChatRole.USER,
                        content = "You are my tutor… huge rendered prompt",
                        timestamp = 0L,
                    ),
                    ChatMessage(
                        id = 2L,
                        conversationId = 1L,
                        role = ChatRole.ASSISTANT,
                        content = "It is a greeting.",
                        timestamp = 1L,
                    ),
                    ChatMessage(
                        id = 3L,
                        conversationId = 1L,
                        role = ChatRole.USER,
                        content = "A follow-up question",
                        timestamp = 2L,
                    ),
                ),
            ),
        )

        composeTestRule
            .onNodeWithText("You are my tutor… huge rendered prompt")
            .assertDoesNotExist()
        composeTestRule.onNodeWithText("It is a greeting.").assertIsDisplayed()
        composeTestRule.onNodeWithText("A follow-up question").assertIsDisplayed()
    }

    @Test
    fun firstUserMessageShownWhenConversationHasNoSource() {
        setChatContent(
            ChatState(
                conversation = conversation,
                messages = listOf(
                    ChatMessage(
                        id = 1L,
                        conversationId = 1L,
                        role = ChatRole.USER,
                        content = "Free question",
                        timestamp = 0L,
                    ),
                ),
            ),
        )

        composeTestRule.onNodeWithText("Free question").assertIsDisplayed()
    }

    @Test
    fun truncatedNoticeShownUnderMessagesWhenReplyWasCutShort() {
        setChatContent(
            ChatState(
                conversation = conversation,
                messages = listOf(
                    ChatMessage(
                        id = 1L,
                        conversationId = 1L,
                        role = ChatRole.ASSISTANT,
                        content = "Cut mid-sen",
                        timestamp = 0L,
                    ),
                ),
                truncated = true,
            ),
        )

        composeTestRule.onNodeWithTag("chat_truncated_notice").assertIsDisplayed()
        composeTestRule
            .onNodeWithText("Response was cut short by the length limit")
            .assertIsDisplayed()
    }

    @Test
    fun truncatedNoticeAbsentByDefault() {
        setChatContent(ChatState(conversation))

        composeTestRule.onNodeWithTag("chat_truncated_notice").assertDoesNotExist()
    }

    @Test
    fun assistantMarkdownRendersHorizontalRuleAndBlockquote() {
        setChatContent(
            ChatState(
                conversation = conversation,
                messages = listOf(
                    ChatMessage(
                        id = 1L,
                        conversationId = 1L,
                        role = ChatRole.ASSISTANT,
                        content = "Before the rule\n\n---\n\n> quoted words",
                        timestamp = 0L,
                    ),
                ),
            ),
        )

        composeTestRule.onNodeWithText("Before the rule").assertIsDisplayed()
        composeTestRule.onNodeWithTag("markdown_blockquote").assertIsDisplayed()
        composeTestRule.onNodeWithText("quoted words").assertIsDisplayed()
        composeTestRule.onNodeWithText("---").assertDoesNotExist()
        composeTestRule.onNodeWithText("> quoted words").assertDoesNotExist()
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
    fun lateFuriganaResolutionRemeasuresMessageSoBottomStaysReachable() {
        val unlock = CompletableDeferred<Unit>()
        var resolved = false
        val tokens = listOf(
            FuriganaToken(
                surface = "漢字",
                reading = "カンジ",
                parts = listOf(FuriganaPart.kanji("漢字", "かんじ")),
            ),
        )
        composeTestRule.setContent {
            LazyColumn(
                modifier = Modifier
                    .height(200.dp)
                    .testTag("chat_clip_list"),
            ) {
                item {
                    MarkdownMessageContent(
                        text = "漢字\n\n漢字\n\n漢字\n\n漢字",
                        isComplete = true,
                        resolveFurigana = { _ ->
                            unlock.await()
                            resolved = true
                            tokens
                        },
                        modifier = Modifier.testTag("late_furigana_message"),
                    )
                }
                item {
                    Text(
                        text = "conversation end",
                        modifier = Modifier.testTag("bottom_marker"),
                    )
                }
            }
        }
        composeTestRule.waitForIdle()
        val heightBeforeRuby = composeTestRule
            .onNodeWithTag("late_furigana_message")
            .getUnclippedBoundsInRoot().height

        unlock.complete(Unit)
        composeTestRule.waitUntil(timeoutMillis = 5_000) { resolved }
        composeTestRule.waitForIdle()

        // Ruby annotations raise every line, so the TextViews grow after the
        // Compose measure pass. Without a content-keyed remeasure the node keeps
        // its stale (pre-ruby) height and the bottom of the message is clipped.
        val heightAfterRuby = composeTestRule
            .onNodeWithTag("late_furigana_message")
            .getUnclippedBoundsInRoot().height
        assertTrue(
            "message must remeasure taller once furigana rubies arrive " +
                "(before=$heightBeforeRuby, after=$heightAfterRuby)",
            heightAfterRuby > heightBeforeRuby,
        )

        // Scrolling to the end must bring the content below the grown message
        // fully into view instead of leaving it cut off past the viewport.
        composeTestRule.onNodeWithTag("chat_clip_list")
            .performScrollToNode(hasTestTag("bottom_marker"))
        composeTestRule.onNodeWithTag("bottom_marker").assertIsDisplayed()
    }

    @Test
    fun backButtonInvokesCallback() {
        var backed = false
        setChatContent(ChatState(conversation), ChatCallbacks(onBack = { backed = true }))

        composeTestRule.onNodeWithContentDescription("Back").performClick()

        assertTrue(backed)
    }
}
