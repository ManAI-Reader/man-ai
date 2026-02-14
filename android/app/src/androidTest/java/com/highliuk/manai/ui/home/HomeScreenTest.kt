package com.highliuk.manai.ui.home

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.highliuk.manai.domain.model.Manga
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class HomeScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun emptyState_showsTitleAndFab() {
        composeTestRule.setContent {
            HomeScreen(
                mangaList = emptyList(),
                onImportClick = {},
                onSettingsClick = {}
            )
        }

        composeTestRule.onNodeWithText("慢愛").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Import PDF").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Settings").assertIsDisplayed()
    }

    @Test
    fun withMangaItems_showsTitlesAndPageCounts() {
        val mangas = listOf(
            Manga(id = 1, uri = "uri1", title = "One Piece", pageCount = 200),
            Manga(id = 2, uri = "uri2", title = "Naruto", pageCount = 150)
        )

        composeTestRule.setContent {
            HomeScreen(
                mangaList = mangas,
                onImportClick = {},
                onSettingsClick = {}
            )
        }

        composeTestRule.onNodeWithText("One Piece").assertIsDisplayed()
        composeTestRule.onNodeWithText("200 pages").assertIsDisplayed()
        composeTestRule.onNodeWithText("Naruto").assertIsDisplayed()
        composeTestRule.onNodeWithText("150 pages").assertIsDisplayed()
    }

    @Test
    fun fabClick_triggersCallback() {
        var clicked = false

        composeTestRule.setContent {
            HomeScreen(
                mangaList = emptyList(),
                onImportClick = { clicked = true },
                onSettingsClick = {}
            )
        }

        composeTestRule.onNodeWithContentDescription("Import PDF").performClick()
        assertTrue(clicked)
    }

    @Test
    fun settingsClick_triggersCallback() {
        var clicked = false

        composeTestRule.setContent {
            HomeScreen(
                mangaList = emptyList(),
                onImportClick = {},
                onSettingsClick = { clicked = true }
            )
        }

        composeTestRule.onNodeWithContentDescription("Settings").performClick()
        assertTrue(clicked)
    }
}
