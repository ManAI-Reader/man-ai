package com.highliuk.manai.ui.reader

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test

class ReaderBottomBarTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun displaysPageIndicator_withCorrectFormat() {
        composeTestRule.setContent {
            ReaderBottomBar(
                currentPage = 0,
                pageCount = 15,
                onPageSelected = {}
            )
        }
        composeTestRule.onNodeWithText("1 / 15").assertIsDisplayed()
    }

    @Test
    fun displaysPageIndicator_forMiddlePage() {
        composeTestRule.setContent {
            ReaderBottomBar(
                currentPage = 4,
                pageCount = 10,
                onPageSelected = {}
            )
        }
        composeTestRule.onNodeWithText("5 / 10").assertIsDisplayed()
    }

    @Test
    fun slider_isDisplayed_whenMultiplePages() {
        composeTestRule.setContent {
            ReaderBottomBar(
                currentPage = 0,
                pageCount = 15,
                onPageSelected = {}
            )
        }
        composeTestRule.onNodeWithTag("page_slider").assertIsDisplayed()
    }

    @Test
    fun slider_isNotDisplayed_forSinglePage() {
        composeTestRule.setContent {
            ReaderBottomBar(
                currentPage = 0,
                pageCount = 1,
                onPageSelected = {}
            )
        }
        composeTestRule.onNodeWithTag("page_slider").assertDoesNotExist()
    }
}
