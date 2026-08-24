package com.highliuk.manai.ui.prompts

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
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

class PromptEditScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val newTemplate = PromptTemplate(id = 0L, name = "", template = "")

    private val existingTemplate = PromptTemplate(
        id = 5L,
        name = "Explain grammar",
        template = "Explain the grammar of {text}",
    )

    private fun setScreenContent(
        template: PromptTemplate? = newTemplate,
        errorRes: Int? = null,
        onSave: (String, String, ReasoningLevel) -> Unit = { _, _, _ -> },
        onBack: () -> Unit = {},
    ) {
        composeTestRule.setContent {
            PromptEditScreen(
                template = template,
                errorRes = errorRes,
                onSave = onSave,
                onBack = onBack,
            )
        }
    }

    @Test
    fun showsAddTitleForNewTemplate() {
        setScreenContent(template = newTemplate)

        composeTestRule.onNodeWithText("Add prompt").assertIsDisplayed()
    }

    @Test
    fun showsEditTitleForExistingTemplate() {
        setScreenContent(template = existingTemplate)

        composeTestRule.onNodeWithText("Edit prompt").assertIsDisplayed()
    }

    @Test
    fun fieldsPrefilledWithTemplateValues() {
        setScreenContent(template = existingTemplate)

        composeTestRule.onNodeWithTag("prompt_name_field")
            .assertTextContains("Explain grammar")
        composeTestRule.onNodeWithTag("prompt_template_field")
            .assertTextContains("Explain the grammar of {text}")
    }

    @Test
    fun placeholdersHintDisplayed() {
        setScreenContent()

        composeTestRule.onNodeWithText(
            "Placeholders: {text} = balloon text, {selection} = selected text, " +
                "{translation} = translation, {title} = manga title, " +
                "{balloons} = other balloons on this page, " +
                "{prev_balloons} = balloons on the previous page"
        ).assertIsDisplayed()
    }

    @Test
    fun errorDisplayedWhenErrorResProvided() {
        setScreenContent(errorRes = R.string.prompt_name_required)

        composeTestRule.onNodeWithTag("prompt_edit_error")
            .performScrollTo()
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("Name and prompt text are required").assertExists()
    }

    @Test
    fun noErrorShownWhenErrorResNull() {
        setScreenContent(errorRes = null)

        composeTestRule.onNodeWithTag("prompt_edit_error").assertDoesNotExist()
    }

    @Test
    fun saveInvokesOnSaveWithEditedValues() {
        var savedName: String? = null
        var savedTemplate: String? = null
        var savedLevel: ReasoningLevel? = null
        setScreenContent(
            template = newTemplate,
            onSave = { name, template, level ->
                savedName = name
                savedTemplate = template
                savedLevel = level
            },
        )

        composeTestRule.onNodeWithTag("prompt_name_field").performTextInput("My prompt")
        composeTestRule.onNodeWithTag("prompt_template_field").performTextInput("Explain {text}")
        composeTestRule.onNodeWithTag("save_prompt").performClick()

        assertEquals("My prompt", savedName)
        assertEquals("Explain {text}", savedTemplate)
        assertEquals(ReasoningLevel.DEFAULT, savedLevel)
    }

    @Test
    fun saveWithClearedNamePassesBlankToCallback() {
        var savedName: String? = null
        setScreenContent(
            template = existingTemplate,
            onSave = { name, _, _ -> savedName = name },
        )

        composeTestRule.onNodeWithTag("prompt_name_field").performTextClearance()
        composeTestRule.onNodeWithTag("save_prompt").performClick()

        assertEquals("", savedName)
    }

    @Test
    fun reasoningSelectorShowsAllLevelsVerticallyWithModelDefaultPreselected() {
        setScreenContent(template = newTemplate)

        ReasoningLevel.entries.forEach { level ->
            composeTestRule.onNodeWithTag("reasoning_radio_${level.name}").assertExists()
        }
        composeTestRule.onNodeWithTag("reasoning_radio_DEFAULT").assertIsSelected()
    }

    @Test
    fun reasoningSelectorPreselectsTemplateLevel() {
        setScreenContent(template = existingTemplate.copy(reasoningLevel = ReasoningLevel.MEDIUM))

        composeTestRule.onNodeWithTag("reasoning_radio_MEDIUM").assertIsSelected()
        composeTestRule.onNodeWithTag("reasoning_radio_DEFAULT").assertIsNotSelected()
    }

    @Test
    fun selectingReasoningLevelIsReflectedInSaveCallback() {
        var savedLevel: ReasoningLevel? = null
        setScreenContent(
            template = existingTemplate,
            onSave = { _, _, level -> savedLevel = level },
        )

        composeTestRule.onNodeWithTag("reasoning_radio_HIGH").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("reasoning_radio_HIGH").assertIsSelected()
        composeTestRule.onNodeWithTag("save_prompt").performClick()

        assertEquals(ReasoningLevel.HIGH, savedLevel)
    }

    @Test
    fun backButtonInvokesOnBack() {
        var backed = false
        setScreenContent(onBack = { backed = true })

        composeTestRule.onNodeWithContentDescription("Back").performClick()

        assertTrue(backed)
    }
}
