package com.highliuk.manai.ui.home

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.highliuk.manai.domain.model.Manga
import org.junit.Rule
import org.junit.Test

class MangaGridItemTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun displaysTitle() {
        composeTestRule.setContent {
            MangaGridItem(manga = Manga(id = 1, uri = "uri1", title = "Frieren", pageCount = 200))
        }

        composeTestRule.onNodeWithText("Frieren").assertIsDisplayed()
    }

    @Test
    fun doesNotDisplayPageCount() {
        composeTestRule.setContent {
            MangaGridItem(manga = Manga(id = 1, uri = "uri1", title = "Frieren", pageCount = 200))
        }

        composeTestRule.onNodeWithText("200 pages").assertDoesNotExist()
    }
}
