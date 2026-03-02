package com.highliuk.manai.ui.reader

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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

    @Test
    fun pageIndicator_callsOnPageIndicatorClick_whenTapped() {
        var clicked = false
        composeTestRule.setContent {
            ReaderBottomBar(
                currentPage = 0,
                pageCount = 15,
                onPageSelected = {},
                onPageIndicatorClick = { clicked = true }
            )
        }
        composeTestRule.onNodeWithTag("page_indicator").performClick()
        assertTrue(clicked)
    }

    @Test
    fun slider_hasRtlLayout_whenIsRtlTrue() {
        composeTestRule.setContent {
            ReaderBottomBar(
                currentPage = 0,
                pageCount = 15,
                isRtl = true,
                onPageSelected = {}
            )
        }
        composeTestRule.onNodeWithTag("page_slider_rtl").assertIsDisplayed()
    }

    @Test
    fun draggingSlider_callsOnPageSelectedWithNewPage() {
        var selectedPage = -1
        composeTestRule.setContent {
            ReaderBottomBar(
                currentPage = 0,
                pageCount = 10,
                onPageSelected = { selectedPage = it }
            )
        }

        composeTestRule.onNodeWithTag("page_slider").performTouchInput {
            down(centerLeft)
            moveTo(center)
            up()
        }
        composeTestRule.waitForIdle()

        assertTrue("onPageSelected should be called with a page > 0", selectedPage > 0)
    }

    @Test
    fun draggingSlider_showsDragPreviewPage() {
        composeTestRule.setContent {
            ReaderBottomBar(
                currentPage = 0,
                pageCount = 10,
                onPageSelected = {}
            )
        }

        composeTestRule.onNodeWithTag("page_slider").performTouchInput {
            down(centerLeft)
            moveTo(centerRight)
        }

        // While dragging, page indicator should show the dragged position
        composeTestRule.onNodeWithText("1 / 10").assertDoesNotExist()

        composeTestRule.onNodeWithTag("page_slider").performTouchInput {
            up()
        }
    }
}
