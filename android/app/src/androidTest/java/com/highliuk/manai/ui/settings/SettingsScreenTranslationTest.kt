package com.highliuk.manai.ui.settings

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.highliuk.manai.domain.model.AppLanguage
import com.highliuk.manai.domain.model.ReadingMode
import com.highliuk.manai.domain.model.TargetLanguage
import com.highliuk.manai.domain.model.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class SettingsScreenTranslationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun displaysLandscapeGridColumnsSection() {
        composeTestRule.setContent {
            SettingsScreen(
                gridColumns = 2,
                onGridColumnsChange = {},
                gridColumnsLandscape = 5,
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
                deeplApiKey = "",
                onDeeplApiKeyChange = {},
                translationTargetLang = TargetLanguage.EN,
                onTranslationTargetLangChange = {},
                onBack = {}
            )
        }

        composeTestRule.onNodeWithText("Grid Columns (Landscape)").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("4 columns").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("5 columns").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("6 columns").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun selectingLandscapeColumnCallsCallback() {
        var selectedColumns = 0

        composeTestRule.setContent {
            SettingsScreen(
                gridColumns = 2,
                onGridColumnsChange = {},
                gridColumnsLandscape = 5,
                onGridColumnsLandscapeChange = { selectedColumns = it },
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
                deeplApiKey = "",
                onDeeplApiKeyChange = {},
                translationTargetLang = TargetLanguage.EN,
                onTranslationTargetLangChange = {},
                onBack = {}
            )
        }

        composeTestRule.onNodeWithText("4 columns").performScrollTo().performClick()

        assertEquals(4, selectedColumns)
    }

    @Test
    fun translationSectionHeaderDisplayed() {
        composeTestRule.setContent {
            SettingsScreen(
                gridColumns = 2,
                onGridColumnsChange = {},
                gridColumnsLandscape = 5,
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
                deeplApiKey = "",
                onDeeplApiKeyChange = {},
                translationTargetLang = TargetLanguage.EN,
                onTranslationTargetLangChange = {},
                onBack = {},
            )
        }

        composeTestRule.onNodeWithText("Translation").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun deeplApiKeyFieldDisplayed() {
        composeTestRule.setContent {
            SettingsScreen(
                gridColumns = 2,
                onGridColumnsChange = {},
                gridColumnsLandscape = 5,
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
                deeplApiKey = "",
                onDeeplApiKeyChange = {},
                translationTargetLang = TargetLanguage.EN,
                onTranslationTargetLangChange = {},
                onBack = {},
            )
        }

        composeTestRule.onNodeWithTag("deepl_api_key_field").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun targetLanguageSectionDisplayed() {
        composeTestRule.setContent {
            SettingsScreen(
                gridColumns = 2,
                onGridColumnsChange = {},
                gridColumnsLandscape = 5,
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
                deeplApiKey = "",
                onDeeplApiKeyChange = {},
                translationTargetLang = TargetLanguage.EN,
                onTranslationTargetLangChange = {},
                onBack = {},
            )
        }

        composeTestRule.onNodeWithText("Target Language").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun selectingTargetLanguageCallsCallback() {
        var selected: TargetLanguage? = null
        composeTestRule.setContent {
            SettingsScreen(
                gridColumns = 2,
                onGridColumnsChange = {},
                gridColumnsLandscape = 5,
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
                deeplApiKey = "",
                onDeeplApiKeyChange = {},
                translationTargetLang = TargetLanguage.EN,
                onTranslationTargetLangChange = { selected = it },
                onBack = {},
            )
        }

        composeTestRule.onNodeWithTag("target_lang_IT").performScrollTo().performClick()

        assertEquals(TargetLanguage.IT, selected)
    }

    @Test
    fun targetLanguagesShowNativeDisplayNames() {
        composeTestRule.setContent {
            SettingsScreen(
                gridColumns = 2,
                onGridColumnsChange = {},
                gridColumnsLandscape = 5,
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
                deeplApiKey = "",
                onDeeplApiKeyChange = {},
                translationTargetLang = TargetLanguage.EN,
                onTranslationTargetLangChange = {},
                onBack = {},
            )
        }

        composeTestRule.onNodeWithTag("target_lang_EN").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("target_lang_IT").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("target_lang_PT-BR").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("target_lang_ZH").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("target_lang_KO").performScrollTo().assertIsDisplayed()
    }
}
