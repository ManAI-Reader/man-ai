package com.highliuk.manai.ui.prompts

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import com.highliuk.manai.R
import com.highliuk.manai.domain.model.PromptTemplate
import com.highliuk.manai.domain.model.ReasoningLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class PromptEditDialogTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val newTemplate = PromptTemplate(id = 0L, name = "", template = "")

    private val existingTemplate = PromptTemplate(
        id = 5L,
        name = "Explain grammar",
        template = "Explain the grammar of {text}",
    )

    private fun setDialogContent(
        template: PromptTemplate = newTemplate,
        errorRes: Int? = null,
        onConfirm: (String, String, ReasoningLevel) -> Unit = { _, _, _ -> },
        onDismiss: () -> Unit = {},
    ) {
        composeTestRule.setContent {
            PromptEditDialog(
                template = template,
                errorRes = errorRes,
                onConfirm = onConfirm,
                onDismiss = onDismiss,
            )
        }
    }

    @Test
    fun showsAddTitleForNewTemplate() {
        setDialogContent(template = newTemplate)

        composeTestRule.onNodeWithText("Add prompt").assertIsDisplayed()
    }

    @Test
    fun showsEditTitleForExistingTemplate() {
        setDialogContent(template = existingTemplate)

        composeTestRule.onNodeWithText("Edit prompt").assertIsDisplayed()
    }

    @Test
    fun fieldsPrefilledWithTemplateValues() {
        setDialogContent(template = existingTemplate)

        composeTestRule.onNodeWithTag("prompt_name_field")
            .assertTextContains("Explain grammar")
        composeTestRule.onNodeWithTag("prompt_template_field")
            .assertTextContains("Explain the grammar of {text}")
    }

    @Test
    fun placeholdersHintDisplayed() {
        setDialogContent()

        composeTestRule.onNodeWithText(
            "Placeholders: {text} = balloon text, {selection} = selected text, " +
                "{translation} = translation, {title} = manga title, " +
                "{balloons} = other balloons on this page, " +
                "{prev_balloons} = balloons on the previous page"
        ).assertIsDisplayed()
    }

    @Test
    fun errorDisplayedWhenErrorResProvided() {
        setDialogContent(errorRes = R.string.prompt_name_required)

        composeTestRule.onNodeWithTag("prompt_edit_error").assertIsDisplayed()
        composeTestRule.onNodeWithText("Name and prompt text are required").assertIsDisplayed()
    }

    @Test
    fun noErrorShownWhenErrorResNull() {
        setDialogContent(errorRes = null)

        composeTestRule.onNodeWithTag("prompt_edit_error").assertDoesNotExist()
    }

    @Test
    fun confirmInvokesOnConfirmWithEditedValues() {
        var savedName: String? = null
        var savedTemplate: String? = null
        var savedLevel: ReasoningLevel? = null
        setDialogContent(
            template = newTemplate,
            onConfirm = { name, template, level ->
                savedName = name
                savedTemplate = template
                savedLevel = level
            },
        )

        composeTestRule.onNodeWithTag("prompt_name_field").performTextInput("My prompt")
        composeTestRule.onNodeWithTag("prompt_template_field").performTextInput("Explain {text}")
        composeTestRule.onNodeWithText("OK").performClick()

        assertEquals("My prompt", savedName)
        assertEquals("Explain {text}", savedTemplate)
        assertEquals(ReasoningLevel.DEFAULT, savedLevel)
    }

    @Test
    fun confirmWithBlankNamePassesBlankToCallback() {
        var savedName: String? = null
        setDialogContent(
            template = existingTemplate,
            onConfirm = { name, _, _ -> savedName = name },
        )

        composeTestRule.onNodeWithTag("prompt_name_field").performTextClearance()
        composeTestRule.onNodeWithText("OK").performClick()

        assertEquals("", savedName)
    }

    @Test
    fun reasoningSelectorShowsAllLevelsWithModelDefaultPreselected() {
        setDialogContent(template = newTemplate)

        ReasoningLevel.entries.forEach { level ->
            composeTestRule.onNodeWithTag("reasoning_chip_${level.name}").assertExists()
        }
        composeTestRule.onNodeWithTag("reasoning_chip_DEFAULT").assertIsSelected()
    }

    @Test
    fun reasoningSelectorPreselectsTemplateLevel() {
        setDialogContent(template = existingTemplate.copy(reasoningLevel = ReasoningLevel.MEDIUM))

        composeTestRule.onNodeWithTag("reasoning_chip_MEDIUM").assertIsSelected()
        composeTestRule.onNodeWithTag("reasoning_chip_DEFAULT").assertIsNotSelected()
    }

    @Test
    fun selectingReasoningChipIsReflectedInConfirmCallback() {
        var savedLevel: ReasoningLevel? = null
        setDialogContent(
            template = existingTemplate,
            onConfirm = { _, _, level -> savedLevel = level },
        )

        composeTestRule.onNodeWithTag("reasoning_chip_HIGH").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("reasoning_chip_HIGH").assertIsSelected()
        composeTestRule.onNodeWithText("OK").performClick()

        assertEquals(ReasoningLevel.HIGH, savedLevel)
    }

    @Test
    fun cancelInvokesOnDismiss() {
        var dismissed = false
        setDialogContent(onDismiss = { dismissed = true })

        composeTestRule.onNodeWithText("Cancel").performClick()

        assertTrue(dismissed)
    }
}
