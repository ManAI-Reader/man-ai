package com.highliuk.manai.ui.settings

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import com.highliuk.manai.domain.model.AppLanguage
import com.highliuk.manai.domain.model.ReadingMode
import com.highliuk.manai.domain.model.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SettingsScreenAiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private data class AiArgs(
        val llmApiKey: String = "",
        val onLlmApiKeyChange: (String) -> Unit = {},
        val llmBaseUrl: String = "",
        val onLlmBaseUrlChange: (String) -> Unit = {},
        val llmModel: String = "",
        val onLlmModelChange: (String) -> Unit = {},
        val onManagePromptsClick: () -> Unit = {},
    )

    private fun setSettingsContent(args: AiArgs = AiArgs()) {
        composeTestRule.setContent {
            SettingsScreen(
                gridColumns = 2,
                onGridColumnsChange = {},
                gridColumnsLandscape = 4,
                onGridColumnsLandscapeChange = {},
                readingMode = ReadingMode.LTR,
                onReadingModeChange = {},
                themeMode = ThemeMode.SYSTEM,
                onThemeModeChange = {},
                appLanguage = AppLanguage.SYSTEM,
                onAppLanguageChange = {},
                tapToNavigatePortrait = false,
                onTapToNavigatePortraitChange = {},
                tapToNavigateLandscape = true,
                onTapToNavigateLandscapeChange = {},
                llmApiKey = args.llmApiKey,
                onLlmApiKeyChange = args.onLlmApiKeyChange,
                llmBaseUrl = args.llmBaseUrl,
                onLlmBaseUrlChange = args.onLlmBaseUrlChange,
                llmModel = args.llmModel,
                onLlmModelChange = args.onLlmModelChange,
                onManagePromptsClick = args.onManagePromptsClick,
                onBack = {},
            )
        }
    }

    @Test
    fun aiSectionHeaderDisplayed() {
        setSettingsContent()

        composeTestRule.onNodeWithText("AI Assistant").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun llmFieldsDisplayed() {
        setSettingsContent()

        composeTestRule.onNodeWithTag("llm_api_key_field").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("llm_base_url_field").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("llm_model_field").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun typingApiKeyInvokesCallback() {
        var typed: String? = null
        setSettingsContent(AiArgs(onLlmApiKeyChange = { typed = it }))

        composeTestRule.onNodeWithTag("llm_api_key_field")
            .performScrollTo()
            .performTextInput("sk-test")

        assertEquals("sk-test", typed)
    }

    @Test
    fun typingBaseUrlInvokesCallback() {
        var typed: String? = null
        setSettingsContent(AiArgs(onLlmBaseUrlChange = { typed = it }))

        composeTestRule.onNodeWithTag("llm_base_url_field")
            .performScrollTo()
            .performTextInput("https://api.example.com")

        assertEquals("https://api.example.com", typed)
    }

    @Test
    fun typingModelInvokesCallback() {
        var typed: String? = null
        setSettingsContent(AiArgs(onLlmModelChange = { typed = it }))

        composeTestRule.onNodeWithTag("llm_model_field")
            .performScrollTo()
            .performTextInput("gpt-4o-mini")

        assertEquals("gpt-4o-mini", typed)
    }

    @Test
    fun apiKeyMaskedByDefaultAndToggleReveals() {
        setSettingsContent(AiArgs(llmApiKey = "secret123"))

        composeTestRule.onNodeWithTag("llm_api_key_field").performScrollTo()
        composeTestRule.onNodeWithText("secret123").assertDoesNotExist()

        composeTestRule.onNode(
            hasAnyAncestor(hasTestTag("llm_api_key_field")) and
                SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button),
            useUnmergedTree = true,
        ).performClick()

        composeTestRule.onNodeWithTag("llm_api_key_field").assertTextContains("secret123")
    }

    @Test
    fun managePromptsButtonInvokesCallback() {
        var clicked = false
        setSettingsContent(AiArgs(onManagePromptsClick = { clicked = true }))

        composeTestRule.onNodeWithText("Manage prompts").performScrollTo().performClick()

        assertTrue(clicked)
    }
}
