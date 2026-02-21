package com.highliuk.manai.ui.home

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.highliuk.manai.domain.model.Manga
import org.junit.Rule
import org.junit.Test

class HomeScreenE2ETest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun homepageDisplaysMangaInGridLayout() {
        val mangas = listOf(
            Manga(id = 1, uri = "uri1", title = "Frieren Vol 1", pageCount = 200),
            Manga(id = 2, uri = "uri2", title = "Spy x Family Vol 1", pageCount = 150),
            Manga(id = 3, uri = "uri3", title = "Jujutsu Kaisen Vol 1", pageCount = 180)
        )

        composeTestRule.setContent {
            HomeScreen(
                mangaList = mangas,
                gridColumns = 2,
                onImportClick = {},
                onSettingsClick = {}
            )
        }

        composeTestRule.onNodeWithText("Frieren Vol 1").assertIsDisplayed()
        composeTestRule.onNodeWithText("Spy x Family Vol 1").assertIsDisplayed()
        // Page count should NOT be displayed in grid view
        composeTestRule.onNodeWithText("200 pages").assertDoesNotExist()
        composeTestRule.onNodeWithText("150 pages").assertDoesNotExist()
    }

    @Test
    fun homepageDisplaysMangaWith3Columns() {
        val mangas = listOf(
            Manga(id = 1, uri = "uri1", title = "Manga A", pageCount = 10),
            Manga(id = 2, uri = "uri2", title = "Manga B", pageCount = 20),
            Manga(id = 3, uri = "uri3", title = "Manga C", pageCount = 30)
        )

        composeTestRule.setContent {
            HomeScreen(
                mangaList = mangas,
                gridColumns = 3,
                onImportClick = {},
                onSettingsClick = {}
            )
        }

        composeTestRule.onNodeWithText("Manga A").assertIsDisplayed()
        composeTestRule.onNodeWithText("Manga B").assertIsDisplayed()
        composeTestRule.onNodeWithText("Manga C").assertIsDisplayed()
    }
}
