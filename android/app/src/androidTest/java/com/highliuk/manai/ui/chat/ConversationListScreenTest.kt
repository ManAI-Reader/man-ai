package com.highliuk.manai.ui.chat

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.highliuk.manai.domain.model.Conversation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ConversationListScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val grammarConversation = Conversation(
        id = 1L,
        title = "Explain grammar",
        createdAt = 100L,
        updatedAt = System.currentTimeMillis(),
    )

    private val vocabularyConversation = Conversation(
        id = 2L,
        title = "Vocabulary",
        createdAt = 300L,
        updatedAt = System.currentTimeMillis(),
    )

    private data class ConversationListArgs(
        val conversations: List<Conversation> = emptyList(),
        val pendingDelete: Conversation? = null,
        val onConversationClick: (Long) -> Unit = {},
        val onDeleteClick: (Conversation) -> Unit = {},
        val onConfirmDelete: () -> Unit = {},
        val onDismissDelete: () -> Unit = {},
        val onBack: () -> Unit = {},
    )

    private fun setConversationListContent(args: ConversationListArgs) {
        composeTestRule.setContent {
            ConversationListScreen(
                conversations = args.conversations,
                pendingDelete = args.pendingDelete,
                onConversationClick = args.onConversationClick,
                onDeleteClick = args.onDeleteClick,
                onConfirmDelete = args.onConfirmDelete,
                onDismissDelete = args.onDismissDelete,
                onBack = args.onBack,
            )
        }
    }

    @Test
    fun emptyStateShowsNoConversationsMessage() {
        setConversationListContent(ConversationListArgs())

        composeTestRule.onNodeWithText("No conversations yet").assertIsDisplayed()
        composeTestRule.onNodeWithTag("conversation_list").assertDoesNotExist()
    }

    @Test
    fun conversationsAreRendered() {
        setConversationListContent(
            ConversationListArgs(
                conversations = listOf(grammarConversation, vocabularyConversation)
            )
        )

        composeTestRule.onNodeWithTag("conversation_list").assertIsDisplayed()
        composeTestRule.onNodeWithText("Explain grammar").assertIsDisplayed()
        composeTestRule.onNodeWithText("Vocabulary").assertIsDisplayed()
        composeTestRule.onNodeWithText("No conversations yet").assertDoesNotExist()
    }

    @Test
    fun clickingConversationInvokesOnConversationClickWithId() {
        var clickedId: Long? = null
        setConversationListContent(
            ConversationListArgs(
                conversations = listOf(grammarConversation, vocabularyConversation),
                onConversationClick = { clickedId = it },
            )
        )

        composeTestRule.onNodeWithText("Vocabulary").performClick()

        assertEquals(2L, clickedId)
    }

    @Test
    fun deleteIconInvokesOnDeleteClickWithConversation() {
        var deleted: Conversation? = null
        setConversationListContent(
            ConversationListArgs(
                conversations = listOf(grammarConversation),
                onDeleteClick = { deleted = it },
            )
        )

        composeTestRule.onNodeWithContentDescription("Delete").performClick()

        assertEquals(grammarConversation, deleted)
    }

    @Test
    fun deleteConfirmationDialogShownWhenPendingDelete() {
        setConversationListContent(
            ConversationListArgs(
                conversations = listOf(grammarConversation),
                pendingDelete = grammarConversation,
            )
        )

        composeTestRule.onNodeWithText("Delete this conversation?").assertIsDisplayed()
    }

    @Test
    fun confirmingDeleteInvokesOnConfirmDelete() {
        var confirmed = false
        setConversationListContent(
            ConversationListArgs(
                conversations = listOf(grammarConversation),
                pendingDelete = grammarConversation,
                onConfirmDelete = { confirmed = true },
            )
        )

        composeTestRule.onNodeWithText("Delete").performClick()

        assertTrue(confirmed)
    }

    @Test
    fun cancellingDeleteInvokesOnDismissDelete() {
        var dismissed = false
        setConversationListContent(
            ConversationListArgs(
                conversations = listOf(grammarConversation),
                pendingDelete = grammarConversation,
                onDismissDelete = { dismissed = true },
            )
        )

        composeTestRule.onNodeWithText("Cancel").performClick()

        assertTrue(dismissed)
    }

    @Test
    fun backButtonInvokesOnBack() {
        var backed = false
        setConversationListContent(ConversationListArgs(onBack = { backed = true }))

        composeTestRule.onNodeWithContentDescription("Back").performClick()

        assertTrue(backed)
    }
}
