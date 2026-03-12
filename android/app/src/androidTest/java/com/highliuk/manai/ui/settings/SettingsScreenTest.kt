package com.highliuk.manai.ui.settings

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.highliuk.manai.domain.model.AppLanguage
import com.highliuk.manai.domain.model.ReadingMode
import com.highliuk.manai.domain.model.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SettingsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun displaysGridColumnsRadioButtons() {
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
                tapToNavigate = false,
                onTapToNavigateChange = {},
                onBack = {}
            )
        }

        composeTestRule.onNodeWithText("Grid Columns").assertIsDisplayed()
        composeTestRule.onNodeWithText("2 columns").assertIsDisplayed()
        composeTestRule.onNodeWithText("3 columns").assertIsDisplayed()
    }

    @Test
    fun selecting3ColumnsCallsCallback() {
        var selectedColumns = 0

        composeTestRule.setContent {
            SettingsScreen(
                gridColumns = 2,
                onGridColumnsChange = { selectedColumns = it },
                gridColumnsLandscape = 5,
                onGridColumnsLandscapeChange = {},
                readingMode = ReadingMode.LTR,
                onReadingModeChange = {},
                themeMode = ThemeMode.SYSTEM,
                onThemeModeChange = {},
                appLanguage = AppLanguage.SYSTEM,
                onAppLanguageChange = {},
                tapToNavigate = false,
                onTapToNavigateChange = {},
                onBack = {}
            )
        }

        composeTestRule.onNodeWithText("3 columns").performClick()

        assertEquals(3, selectedColumns)
    }

    @Test
    fun backArrowCallsOnBack() {
        var backCalled = false

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
                tapToNavigate = false,
                onTapToNavigateChange = {},
                onBack = { backCalled = true }
            )
        }

        composeTestRule.onNodeWithText("Settings").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Back").performClick()

        assertTrue(backCalled)
    }

    @Test
    fun displaysReadingModeSection() {
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
                tapToNavigate = false,
                onTapToNavigateChange = {},
                onBack = {}
            )
        }

        composeTestRule.onNodeWithText("Reading Mode").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Left to Right").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Right to Left").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun clickingRtlCallsCallbackWithRtl() {
        var selectedMode: ReadingMode? = null

        composeTestRule.setContent {
            SettingsScreen(
                gridColumns = 2,
                onGridColumnsChange = {},
                gridColumnsLandscape = 5,
                onGridColumnsLandscapeChange = {},
                readingMode = ReadingMode.LTR,
                onReadingModeChange = { selectedMode = it },
                themeMode = ThemeMode.SYSTEM,
                onThemeModeChange = {},
                appLanguage = AppLanguage.SYSTEM,
                onAppLanguageChange = {},
                tapToNavigate = false,
                onTapToNavigateChange = {},
                onBack = {}
            )
        }

        composeTestRule.onNodeWithText("Right to Left").performScrollTo().performClick()

        assertEquals(ReadingMode.RTL, selectedMode)
    }

    @Test
    fun displaysThemeModeSection() {
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
                tapToNavigate = false,
                onTapToNavigateChange = {},
                onBack = {}
            )
        }

        composeTestRule.onNodeWithText("Theme").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Light").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Dark").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("System").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun clickingDarkThemeCallsCallback() {
        var selectedTheme: ThemeMode? = null

        composeTestRule.setContent {
            SettingsScreen(
                gridColumns = 2,
                onGridColumnsChange = {},
                gridColumnsLandscape = 5,
                onGridColumnsLandscapeChange = {},
                readingMode = ReadingMode.LTR,
                onReadingModeChange = {},
                themeMode = ThemeMode.SYSTEM,
                onThemeModeChange = { selectedTheme = it },
                appLanguage = AppLanguage.SYSTEM,
                onAppLanguageChange = {},
                tapToNavigate = false,
                onTapToNavigateChange = {},
                onBack = {}
            )
        }

        composeTestRule.onNodeWithText("Dark").performScrollTo().performClick()

        assertEquals(ThemeMode.DARK, selectedTheme)
    }

    @Test
    fun displaysLanguageSection() {
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
                tapToNavigate = false,
                onTapToNavigateChange = {},
                onBack = {}
            )
        }

        composeTestRule.onNodeWithText("Language").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("System default").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("English").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Italiano").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun displaysWebtoonReadingModeOption() {
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
                tapToNavigate = false,
                onTapToNavigateChange = {},
                onBack = {}
            )
        }

        composeTestRule.onNodeWithText("Webtoon").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun clickingWebtoonCallsCallbackWithWebtoon() {
        var selectedMode: ReadingMode? = null

        composeTestRule.setContent {
            SettingsScreen(
                gridColumns = 2,
                onGridColumnsChange = {},
                gridColumnsLandscape = 5,
                onGridColumnsLandscapeChange = {},
                readingMode = ReadingMode.LTR,
                onReadingModeChange = { selectedMode = it },
                themeMode = ThemeMode.SYSTEM,
                onThemeModeChange = {},
                appLanguage = AppLanguage.SYSTEM,
                onAppLanguageChange = {},
                tapToNavigate = false,
                onTapToNavigateChange = {},
                onBack = {}
            )
        }

        composeTestRule.onNodeWithText("Webtoon").performScrollTo().performClick()

        assertEquals(ReadingMode.WEBTOON, selectedMode)
    }

    @Test
    fun clickingItalianLanguageCallsCallback() {
        var selectedLanguage: AppLanguage? = null

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
                onAppLanguageChange = { selectedLanguage = it },
                tapToNavigate = false,
                onTapToNavigateChange = {},
                onBack = {}
            )
        }

        composeTestRule.onNodeWithText("Italiano").performScrollTo().performClick()

        assertEquals(AppLanguage.ITALIAN, selectedLanguage)
    }

    @Test
    fun fontSizeSliderIsDisplayed() {
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
                comicTextScale = 1.5f,
                onComicTextScaleChange = {},
                tapToNavigate = false,
                onTapToNavigateChange = {},
                onBack = {},
            )
        }

        composeTestRule.onNodeWithTag("comic_text_scale_slider").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun displaysTapToNavigateSwitch() {
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
                tapToNavigate = false,
                onTapToNavigateChange = {},
                onBack = {}
            )
        }

        composeTestRule.onNodeWithText("Tap to navigate").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Tap left or right edge to change page")
            .performScrollTo().assertIsDisplayed()
    }

    @Test
    fun tappingSwitchCallsCallback() {
        var toggled = false

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
                tapToNavigate = false,
                onTapToNavigateChange = { toggled = true },
                onBack = {}
            )
        }

        composeTestRule.onNodeWithText("Tap to navigate").performScrollTo().performClick()

        assert(toggled)
    }

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
                tapToNavigate = false,
                onTapToNavigateChange = {},
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
                tapToNavigate = false,
                onTapToNavigateChange = {},
                onBack = {}
            )
        }

        composeTestRule.onNodeWithText("4 columns").performScrollTo().performClick()

        assertEquals(4, selectedColumns)
    }
}
