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
import com.highliuk.manai.domain.model.LlmVendor
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
        onSave: OnSavePrompt = { _, _, _, _, _ -> },
        onBack: () -> Unit = {},
    ) {
        composeTestRule.setContent {
            PromptEditScreen(
                template = template,
                errorRes = errorRes,
                onSave = onSave,
                modelForVendorChange = { model, vendor ->
                    if (model.isBlank() || LlmVendor.entries.any {
                            it != vendor && it.defaultModel == model
                        }
                    ) {
                        vendor.defaultModel
                    } else {
                        model
                    }
                },
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
        var savedVendor: LlmVendor? = null
        var savedModel: String? = null
        setScreenContent(
            template = newTemplate,
            onSave = { name, template, level, vendor, model ->
                savedName = name
                savedTemplate = template
                savedLevel = level
                savedVendor = vendor
                savedModel = model
            },
        )

        composeTestRule.onNodeWithTag("prompt_name_field").performTextInput("My prompt")
        composeTestRule.onNodeWithTag("prompt_template_field").performTextInput("Explain {text}")
        composeTestRule.onNodeWithTag("save_prompt").performClick()

        assertEquals("My prompt", savedName)
        assertEquals("Explain {text}", savedTemplate)
        assertEquals(ReasoningLevel.DEFAULT, savedLevel)
        assertEquals(LlmVendor.GROQ, savedVendor)
        assertEquals(LlmVendor.GROQ.defaultModel, savedModel)
    }

    @Test
    fun saveWithClearedNamePassesBlankToCallback() {
        var savedName: String? = null
        setScreenContent(
            template = existingTemplate,
            onSave = { name, _, _, _, _ -> savedName = name },
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
            onSave = { _, _, level, _, _ -> savedLevel = level },
        )

        composeTestRule.onNodeWithTag("reasoning_radio_HIGH").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("reasoning_radio_HIGH").assertIsSelected()
        composeTestRule.onNodeWithTag("save_prompt").performClick()

        assertEquals(ReasoningLevel.HIGH, savedLevel)
    }

    @Test
    fun vendorSelectorShowsBothVendorsWithGroqPreselected() {
        setScreenContent(template = newTemplate)

        composeTestRule.onNodeWithTag("vendor_radio_GROQ").assertExists().assertIsSelected()
        composeTestRule.onNodeWithTag("vendor_radio_DEEPSEEK").assertExists().assertIsNotSelected()
    }

    @Test
    fun vendorSelectorPreselectsTemplateVendor() {
        setScreenContent(
            template = existingTemplate.copy(
                vendor = LlmVendor.DEEPSEEK,
                model = "deepseek-chat",
            ),
        )

        composeTestRule.onNodeWithTag("vendor_radio_DEEPSEEK").assertIsSelected()
        composeTestRule.onNodeWithTag("vendor_radio_GROQ").assertIsNotSelected()
    }

    @Test
    fun modelFieldPrefilledWithTemplateModel() {
        setScreenContent(
            template = existingTemplate.copy(
                vendor = LlmVendor.DEEPSEEK,
                model = "deepseek-reasoner",
            ),
        )

        composeTestRule.onNodeWithTag("prompt_model_field")
            .performScrollTo()
            .assertTextContains("deepseek-reasoner")
    }

    @Test
    fun switchingVendorSwapsTheDefaultModel() {
        setScreenContent(template = newTemplate)

        composeTestRule.onNodeWithTag("vendor_radio_DEEPSEEK").performScrollTo().performClick()

        composeTestRule.onNodeWithTag("prompt_model_field")
            .performScrollTo()
            .assertTextContains(LlmVendor.DEEPSEEK.defaultModel)
    }

    @Test
    fun switchingVendorKeepsACustomizedModel() {
        setScreenContent(template = newTemplate)

        composeTestRule.onNodeWithTag("prompt_model_field").performScrollTo()
            .performTextClearance()
        composeTestRule.onNodeWithTag("prompt_model_field")
            .performTextInput("llama-3.3-70b-versatile")
        composeTestRule.onNodeWithTag("vendor_radio_DEEPSEEK").performScrollTo().performClick()

        composeTestRule.onNodeWithTag("prompt_model_field")
            .performScrollTo()
            .assertTextContains("llama-3.3-70b-versatile")
    }

    @Test
    fun selectedVendorAndModelAreReflectedInSaveCallback() {
        var savedVendor: LlmVendor? = null
        var savedModel: String? = null
        setScreenContent(
            template = existingTemplate,
            onSave = { _, _, _, vendor, model ->
                savedVendor = vendor
                savedModel = model
            },
        )

        composeTestRule.onNodeWithTag("vendor_radio_DEEPSEEK").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("save_prompt").performClick()

        assertEquals(LlmVendor.DEEPSEEK, savedVendor)
        assertEquals(LlmVendor.DEEPSEEK.defaultModel, savedModel)
    }

    @Test
    fun backButtonInvokesOnBack() {
        var backed = false
        setScreenContent(onBack = { backed = true })

        composeTestRule.onNodeWithContentDescription("Back").performClick()

        assertTrue(backed)
    }
}
