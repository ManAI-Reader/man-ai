package com.highliuk.manai.ui.reader

import androidx.activity.ComponentActivity
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import com.highliuk.manai.domain.model.Manga
import com.highliuk.manai.domain.model.PageRegion
import com.highliuk.manai.domain.model.ReadingMode
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ReaderScreenOcrTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private fun advancePastDoubleTapTimeout() {
        composeTestRule.mainClock.advanceTimeBy(500)
    }

    @Test
    fun tappingRegion_showsBottomSheet() {
        val region = PageRegion(0, 0.0f, 0.0f, 1.0f, 1.0f, 0.9f, "\u5168\u753b\u9762")
        val regions = listOf(region)
        val selectedRegion = mutableStateOf<PageRegion?>(null)

        composeTestRule.setContent {
            ReaderScreen(
                manga = Manga(id = 1, uri = "content://test", title = "Test", pageCount = 1),
                currentPage = 0,
                regions = regions,
                selectedRegion = selectedRegion.value,
                onPageChanged = {},
                onRegionTapped = { selectedRegion.value = it },
                onDismissBottomSheet = { selectedRegion.value = null },
                onBack = {},
                onSettingsClick = {},
            )
        }

        composeTestRule.onNodeWithTag("reader_pager").performTouchInput {
            click(position = Offset(x = width * 0.5f, y = height * 0.5f))
        }
        advancePastDoubleTapTimeout()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("\u5168\u753b\u9762").assertIsDisplayed()
    }

    @Test
    fun tappingRegion_invokesCallback_withCorrectRegion() {
        var tappedRegion: PageRegion? = null
        val regions = listOf(
            PageRegion(0, 0.0f, 0.0f, 1.0f, 1.0f, 0.9f, "\u5168\u753b\u9762")
        )

        composeTestRule.setContent {
            ReaderScreen(
                manga = Manga(id = 1, uri = "content://test", title = "Test", pageCount = 1),
                currentPage = 0,
                regions = regions,
                onPageChanged = {},
                onRegionTapped = { tappedRegion = it },
                onBack = {},
                onSettingsClick = {},
            )
        }

        composeTestRule.onNodeWithTag("reader_pager").performTouchInput {
            click(position = Offset(x = width * 0.5f, y = height * 0.5f))
        }
        advancePastDoubleTapTimeout()
        composeTestRule.waitForIdle()

        assertEquals(regions[0], tappedRegion)
    }

    @Test
    fun webtoonMode_bottomSheet_showsCorrectPageRegionText() {
        // Bug: in webtoon, liveRegion lookup used currentPageRegions (page 0)
        // instead of the tapped region's page (page 1). Both pages have regionIndex=0
        // but different OCR text.

        val regionPage0 = PageRegion(
            regionIndex = 0, normX1 = 0f, normY1 = 0f, normX2 = 1f, normY2 = 1f,
            confidence = 0.9f, ocrText = "wrong_page0_text", pageIndex = 0,
        )
        val regionPage1 = PageRegion(
            regionIndex = 0, normX1 = 0f, normY1 = 0f, normX2 = 1f, normY2 = 1f,
            confidence = 0.9f, ocrText = "correct_page1_text", pageIndex = 1,
        )

        // regions = currentPageRegions = page 0's regions
        val currentPageRegions = listOf(regionPage0)
        // selectedRegion is the one from page 1 (tapped in webtoon)
        val selectedRegion = mutableStateOf<PageRegion?>(regionPage1)

        composeTestRule.setContent {
            ReaderScreen(
                manga = Manga(id = 1, uri = "content://test", title = "Test", pageCount = 5),
                currentPage = 0,
                readingMode = ReadingMode.WEBTOON,
                regions = currentPageRegions,
                selectedRegion = selectedRegion.value,
                visiblePagesRegions = mapOf(
                    0 to listOf(regionPage0),
                    1 to listOf(regionPage1),
                ),
                onPageChanged = {},
                onDismissBottomSheet = { selectedRegion.value = null },
                onBack = {},
                onSettingsClick = {},
            )
        }

        composeTestRule.waitForIdle()

        // The bottom sheet must show page 1's text, not page 0's
        composeTestRule.onNodeWithText("correct_page1_text").assertIsDisplayed()
        composeTestRule.onNodeWithText("wrong_page0_text").assertDoesNotExist()
    }
}
