package com.highliuk.manai.ui.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.longClick
import com.highliuk.manai.domain.model.Manga
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class DeleteMangaE2ETest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun longPress_selectManga_delete_removesFromList() {
        var mangaList by mutableStateOf(
            listOf(
                Manga(id = 1, uri = "uri1", title = "Manga 1", pageCount = 200),
                Manga(id = 2, uri = "uri2", title = "Manga 2", pageCount = 150)
            )
        )
        var selectedIds by mutableStateOf(emptySet<Long>())
        var showDialog by mutableStateOf(false)
        var deleted = false

        composeTestRule.setContent {
            val isSelectionMode = selectedIds.isNotEmpty()
            HomeScreen(
                mangaList = mangaList,
                selectedMangaIds = selectedIds,
                isSelectionMode = isSelectionMode,
                onImportClick = {},
                onSettingsClick = {},
                onMangaClick = {},
                onToggleSelection = { id ->
                    selectedIds = if (id in selectedIds) selectedIds - id else selectedIds + id
                },
                onDeleteClick = { showDialog = true },
                onClearSelection = { selectedIds = emptySet() }
            )

            if (showDialog) {
                DeleteMangaDialog(
                    mangaCount = selectedIds.size,
                    onConfirm = {
                        deleted = true
                        mangaList = mangaList.filter { it.id !in selectedIds }
                        selectedIds = emptySet()
                        showDialog = false
                    },
                    onDismiss = { showDialog = false }
                )
            }
        }

        // Verify both manga are displayed
        composeTestRule.onNodeWithText("Manga 1").assertIsDisplayed()
        composeTestRule.onNodeWithText("Manga 2").assertIsDisplayed()

        // Long press to enter selection mode
        composeTestRule.onNodeWithText("Manga 1").performTouchInput { longClick() }
        assertEquals(setOf(1L), selectedIds)

        // Trash icon should appear
        composeTestRule.onNodeWithContentDescription("Delete").assertIsDisplayed()

        // Tap delete → dialog appears
        composeTestRule.onNodeWithContentDescription("Delete").performClick()
        composeTestRule.onNodeWithText("Delete 1 manga?").assertIsDisplayed()

        // Confirm deletion
        composeTestRule.onNodeWithText("Delete").performClick()
        assertTrue(deleted)

        // First manga should be gone, second should remain
        composeTestRule.onNodeWithText("Manga 1").assertDoesNotExist()
        composeTestRule.onNodeWithText("Manga 2").assertIsDisplayed()
    }
}
