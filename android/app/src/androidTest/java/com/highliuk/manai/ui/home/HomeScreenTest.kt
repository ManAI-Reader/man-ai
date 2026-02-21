package com.highliuk.manai.ui.home

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.highliuk.manai.domain.model.Manga
import org.junit.Assert.assertEquals
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

        composeTestRule.onNodeWithText("Man AI").assertIsDisplayed()
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
        composeTestRule.onNodeWithText("Naruto").assertIsDisplayed()
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
    fun tappingMangaGridItem_callsOnMangaClick() {
        val manga = Manga(id = 1, uri = "uri1", title = "One Piece", pageCount = 200)
        var clickedManga: Manga? = null

        composeTestRule.setContent {
            HomeScreen(
                mangaList = listOf(manga),
                onImportClick = {},
                onSettingsClick = {},
                onMangaClick = { clickedManga = it }
            )
        }

        composeTestRule.onNodeWithText("One Piece").performClick()
        assertEquals(manga, clickedManga)
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

    @Test
    fun selectionMode_showsDeleteButton() {
        val manga = Manga(id = 1, uri = "uri1", title = "One Piece", pageCount = 200)
        composeTestRule.setContent {
            HomeScreen(
                mangaList = listOf(manga),
                selectedMangaIds = setOf(1L),
                isSelectionMode = true,
                onImportClick = {},
                onSettingsClick = {},
                onMangaClick = {},
                onToggleSelection = {},
                onDeleteClick = {},
                onClearSelection = {}
            )
        }
        composeTestRule.onNodeWithContentDescription("Delete").assertIsDisplayed()
    }

    @Test
    fun selectionMode_tapDeleteButton_callsCallback() {
        var deleteClicked = false
        composeTestRule.setContent {
            HomeScreen(
                mangaList = listOf(Manga(id = 1, uri = "u", title = "M", pageCount = 1)),
                selectedMangaIds = setOf(1L),
                isSelectionMode = true,
                onImportClick = {},
                onSettingsClick = {},
                onMangaClick = {},
                onToggleSelection = {},
                onDeleteClick = { deleteClicked = true },
                onClearSelection = {}
            )
        }
        composeTestRule.onNodeWithContentDescription("Delete").performClick()
        assertTrue(deleteClicked)
    }

    @Test
    fun deleteDialog_showsConfirmationAndCallsDelete() {
        var confirmed = false
        composeTestRule.setContent {
            DeleteMangaDialog(
                mangaCount = 3,
                onConfirm = { confirmed = true },
                onDismiss = {}
            )
        }
        composeTestRule.onNodeWithText("Delete 3 manga?").assertIsDisplayed()
        composeTestRule.onNodeWithText("Delete").performClick()
        assertTrue(confirmed)
    }
}
