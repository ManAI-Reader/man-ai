package com.highliuk.manai.ui.prompts

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.highliuk.manai.domain.model.PromptTemplate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class PromptListScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val grammarTemplate = PromptTemplate(
        id = 1L,
        name = "Explain grammar",
        template = "Explain the grammar of {text}\nin detail",
    )

    private val translateTemplate = PromptTemplate(
        id = 2L,
        name = "Translate literally",
        template = "Translate {text} literally",
    )

    private data class PromptListArgs(
        val templates: List<PromptTemplate> = emptyList(),
        val pendingDelete: PromptTemplate? = null,
        val onAddClick: () -> Unit = {},
        val onEditClick: (PromptTemplate) -> Unit = {},
        val onDeleteClick: (PromptTemplate) -> Unit = {},
        val onConfirmDelete: () -> Unit = {},
        val onDismissDelete: () -> Unit = {},
        val onBack: () -> Unit = {},
    )

    private fun setPromptListContent(args: PromptListArgs) {
        composeTestRule.setContent {
            PromptListScreen(
                templates = args.templates,
                pendingDelete = args.pendingDelete,
                onAddClick = args.onAddClick,
                onEditClick = args.onEditClick,
                onDeleteClick = args.onDeleteClick,
                onConfirmDelete = args.onConfirmDelete,
                onDismissDelete = args.onDismissDelete,
                onBack = args.onBack,
            )
        }
    }

    @Test
    fun emptyStateShowsNoPromptsMessage() {
        setPromptListContent(PromptListArgs())

        composeTestRule.onNodeWithText("No prompts yet").assertIsDisplayed()
        composeTestRule.onNodeWithTag("prompt_list").assertDoesNotExist()
    }

    @Test
    fun templatesAreRendered() {
        setPromptListContent(
            PromptListArgs(templates = listOf(grammarTemplate, translateTemplate))
        )

        composeTestRule.onNodeWithTag("prompt_list").assertIsDisplayed()
        composeTestRule.onNodeWithText("Explain grammar").assertIsDisplayed()
        composeTestRule.onNodeWithText("Translate literally").assertIsDisplayed()
        composeTestRule.onNodeWithText("Explain the grammar of {text}").assertIsDisplayed()
        composeTestRule.onNodeWithText("No prompts yet").assertDoesNotExist()
    }

    @Test
    fun fabInvokesOnAddClick() {
        var added = false
        setPromptListContent(PromptListArgs(onAddClick = { added = true }))

        composeTestRule.onNodeWithTag("add_prompt_fab").performClick()

        assertTrue(added)
    }

    @Test
    fun tappingARowInvokesOnEditClickWithItsTemplate() {
        var edited: PromptTemplate? = null
        setPromptListContent(
            PromptListArgs(
                templates = listOf(grammarTemplate, translateTemplate),
                onEditClick = { edited = it },
            )
        )

        composeTestRule.onNodeWithTag("prompt_row_2").performClick()

        assertEquals(translateTemplate, edited)
    }

    @Test
    fun editPencilIconIsGoneFromTheRow() {
        setPromptListContent(PromptListArgs(templates = listOf(grammarTemplate)))

        composeTestRule.onNodeWithContentDescription("Edit prompt").assertDoesNotExist()
    }

    @Test
    fun deleteIconInvokesOnDeleteClickWithTemplate() {
        var deleted: PromptTemplate? = null
        setPromptListContent(
            PromptListArgs(
                templates = listOf(grammarTemplate),
                onDeleteClick = { deleted = it },
            )
        )

        composeTestRule.onNodeWithContentDescription("Delete").performClick()

        assertEquals(grammarTemplate, deleted)
    }

    @Test
    fun deleteConfirmationDialogShownWhenPendingDelete() {
        setPromptListContent(
            PromptListArgs(
                templates = listOf(grammarTemplate),
                pendingDelete = grammarTemplate,
            )
        )

        composeTestRule.onNodeWithText("Delete this prompt?").assertIsDisplayed()
    }

    @Test
    fun confirmingDeleteInvokesOnConfirmDelete() {
        var confirmed = false
        setPromptListContent(
            PromptListArgs(
                templates = listOf(grammarTemplate),
                pendingDelete = grammarTemplate,
                onConfirmDelete = { confirmed = true },
            )
        )

        composeTestRule.onNodeWithText("Delete").performClick()

        assertTrue(confirmed)
    }

    @Test
    fun cancellingDeleteInvokesOnDismissDelete() {
        var dismissed = false
        setPromptListContent(
            PromptListArgs(
                templates = listOf(grammarTemplate),
                pendingDelete = grammarTemplate,
                onDismissDelete = { dismissed = true },
            )
        )

        composeTestRule.onNodeWithText("Cancel").performClick()

        assertTrue(dismissed)
    }

    @Test
    fun backButtonInvokesOnBack() {
        var backed = false
        setPromptListContent(PromptListArgs(onBack = { backed = true }))

        composeTestRule.onNodeWithContentDescription("Back").performClick()

        assertTrue(backed)
    }
}
