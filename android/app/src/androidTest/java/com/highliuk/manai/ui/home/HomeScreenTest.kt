package com.highliuk.manai.ui.home

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
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
            Manga(id = 1, uri = "uri1", title = "Manga 1", pageCount = 200),
            Manga(id = 2, uri = "uri2", title = "Manga 2", pageCount = 150)
        )

        composeTestRule.setContent {
            HomeScreen(
                mangaList = mangas,
                onImportClick = {},
                onSettingsClick = {}
            )
        }

        composeTestRule.onNodeWithText("Manga 1").assertIsDisplayed()
        composeTestRule.onNodeWithText("Manga 2").assertIsDisplayed()
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
        val manga = Manga(id = 1, uri = "uri1", title = "Manga 1", pageCount = 200)
        var clickedManga: Manga? = null

        composeTestRule.setContent {
            HomeScreen(
                mangaList = listOf(manga),
                onImportClick = {},
                onSettingsClick = {},
                onMangaClick = { clickedManga = it }
            )
        }

        composeTestRule.onNodeWithText("Manga 1").performClick()
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
        val manga = Manga(id = 1, uri = "uri1", title = "Manga 1", pageCount = 200)
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
    fun selectionMode_singleSelection_showsRenameButton() {
        composeTestRule.setContent {
            HomeScreen(
                mangaList = listOf(Manga(id = 1, uri = "u", title = "M", pageCount = 1)),
                selectedMangaIds = setOf(1L),
                isSelectionMode = true,
                onImportClick = {},
                onSettingsClick = {},
                onRenameClick = {},
                onDeleteClick = {},
                onClearSelection = {}
            )
        }
        composeTestRule.onNodeWithContentDescription("Rename").assertIsDisplayed()
    }

    @Test
    fun selectionMode_multipleSelection_hidesRenameButton() {
        composeTestRule.setContent {
            HomeScreen(
                mangaList = listOf(
                    Manga(id = 1, uri = "u", title = "M", pageCount = 1),
                    Manga(id = 2, uri = "u2", title = "M2", pageCount = 2)
                ),
                selectedMangaIds = setOf(1L, 2L),
                isSelectionMode = true,
                onImportClick = {},
                onSettingsClick = {},
                onRenameClick = {},
                onDeleteClick = {},
                onClearSelection = {}
            )
        }
        composeTestRule.onNodeWithContentDescription("Rename").assertDoesNotExist()
    }

    @Test
    fun selectionMode_tapRenameButton_callsCallback() {
        var renameClicked = false
        composeTestRule.setContent {
            HomeScreen(
                mangaList = listOf(Manga(id = 1, uri = "u", title = "M", pageCount = 1)),
                selectedMangaIds = setOf(1L),
                isSelectionMode = true,
                onImportClick = {},
                onSettingsClick = {},
                onRenameClick = { renameClicked = true },
                onDeleteClick = {},
                onClearSelection = {}
            )
        }
        composeTestRule.onNodeWithContentDescription("Rename").performClick()
        assertTrue(renameClicked)
    }

    @Test
    fun renameDialog_showsTitleFieldAndButtons() {
        composeTestRule.setContent {
            RenameMangaDialog(
                currentTitle = "Old Title",
                onConfirm = {},
                onDismiss = {}
            )
        }
        composeTestRule.onNodeWithText("Rename manga").assertIsDisplayed()
        composeTestRule.onNodeWithText("Old Title").assertIsDisplayed()
        composeTestRule.onNodeWithText("OK").assertIsEnabled()
        composeTestRule.onNodeWithText("Cancel").assertIsDisplayed()
    }

    @Test
    fun renameDialog_emptyText_disablesOkButton() {
        composeTestRule.setContent {
            RenameMangaDialog(
                currentTitle = "Old Title",
                onConfirm = {},
                onDismiss = {}
            )
        }
        composeTestRule.onNodeWithText("Old Title").performTextClearance()
        composeTestRule.onNodeWithText("OK").assertIsNotEnabled()
    }

    @Test
    fun renameDialog_confirmCallsCallbackWithNewTitle() {
        var confirmedTitle = ""
        composeTestRule.setContent {
            RenameMangaDialog(
                currentTitle = "Old Title",
                onConfirm = { confirmedTitle = it },
                onDismiss = {}
            )
        }
        composeTestRule.onNodeWithText("Old Title").performTextClearance()
        composeTestRule.onNodeWithText("Title").performTextInput("New Title")
        composeTestRule.onNodeWithText("OK").performClick()
        assertEquals("New Title", confirmedTitle)
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
