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
        val groqApiKey: String = "",
        val onGroqApiKeyChange: (String) -> Unit = {},
        val deepseekApiKey: String = "",
        val onDeepseekApiKeyChange: (String) -> Unit = {},
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
                groqApiKey = args.groqApiKey,
                onGroqApiKeyChange = args.onGroqApiKeyChange,
                deepseekApiKey = args.deepseekApiKey,
                onDeepseekApiKeyChange = args.onDeepseekApiKeyChange,
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
    fun bothVendorKeyFieldsDisplayed() {
        setSettingsContent()

        composeTestRule.onNodeWithTag("groq_api_key_field").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("deepseek_api_key_field").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun baseUrlAndModelFieldsAreGone() {
        setSettingsContent()

        composeTestRule.onNodeWithTag("llm_base_url_field").assertDoesNotExist()
        composeTestRule.onNodeWithTag("llm_model_field").assertDoesNotExist()
        composeTestRule.onNodeWithTag("llm_api_key_field").assertDoesNotExist()
    }

    @Test
    fun typingGroqApiKeyInvokesCallback() {
        var typed: String? = null
        setSettingsContent(AiArgs(onGroqApiKeyChange = { typed = it }))

        composeTestRule.onNodeWithTag("groq_api_key_field")
            .performScrollTo()
            .performTextInput("gsk-test")

        assertEquals("gsk-test", typed)
    }

    @Test
    fun typingDeepseekApiKeyInvokesCallback() {
        var typed: String? = null
        setSettingsContent(AiArgs(onDeepseekApiKeyChange = { typed = it }))

        composeTestRule.onNodeWithTag("deepseek_api_key_field")
            .performScrollTo()
            .performTextInput("sk-test")

        assertEquals("sk-test", typed)
    }

    @Test
    fun groqApiKeyMaskedByDefaultAndToggleReveals() {
        setSettingsContent(AiArgs(groqApiKey = "secret123"))

        composeTestRule.onNodeWithTag("groq_api_key_field").performScrollTo()
        composeTestRule.onNodeWithText("secret123").assertDoesNotExist()

        composeTestRule.onNode(
            hasAnyAncestor(hasTestTag("groq_api_key_field")) and
                SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button),
            useUnmergedTree = true,
        ).performClick()

        composeTestRule.onNodeWithTag("groq_api_key_field").assertTextContains("secret123")
    }

    @Test
    fun deepseekApiKeyMaskedByDefaultAndToggleReveals() {
        setSettingsContent(AiArgs(deepseekApiKey = "secret456"))

        composeTestRule.onNodeWithTag("deepseek_api_key_field").performScrollTo()
        composeTestRule.onNodeWithText("secret456").assertDoesNotExist()

        composeTestRule.onNode(
            hasAnyAncestor(hasTestTag("deepseek_api_key_field")) and
                SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button),
            useUnmergedTree = true,
        ).performClick()

        composeTestRule.onNodeWithTag("deepseek_api_key_field").assertTextContains("secret456")
    }

    @Test
    fun managePromptsButtonInvokesCallback() {
        var clicked = false
        setSettingsContent(AiArgs(onManagePromptsClick = { clicked = true }))

        composeTestRule.onNodeWithText("Manage prompts").performScrollTo().performClick()

        assertTrue(clicked)
    }
}
