package com.highliuk.manai.ui.settings

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.highliuk.manai.domain.model.AppLanguage
import com.highliuk.manai.domain.model.ReadingMode
import com.highliuk.manai.domain.model.ThemeMode
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SettingsScreenFuriganaTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun furiganaToggleIsDisplayed() {
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
                showFurigana = false,
                onShowFuriganaChange = {},
                onBack = {}
            )
        }

        composeTestRule.onNodeWithText("Furigana").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun furiganaToggleInvokesCallback() {
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
                tapToNavigatePortrait = false,
                onTapToNavigatePortraitChange = {},
                tapToNavigateLandscape = true,
                onTapToNavigateLandscapeChange = {},
                showFurigana = false,
                onShowFuriganaChange = { toggled = true },
                onBack = {}
            )
        }

        composeTestRule.onNodeWithText("Furigana").performScrollTo().performClick()
        assertTrue(toggled)
    }
}
