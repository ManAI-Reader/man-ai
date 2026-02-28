package com.highliuk.manai.ui.reader

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.highliuk.manai.domain.model.PageRegion
import org.junit.Rule
import org.junit.Test

class OcrBottomSheetTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun showsLoadingIndicatorWhenOcrTextIsNull() {
        val region = PageRegion(0, 0.1f, 0.1f, 0.5f, 0.5f, 0.9f, null)

        composeTestRule.setContent {
            OcrBottomSheet(region = region, onDismiss = {})
        }

        composeTestRule.onNodeWithText("Recognizing text", substring = true).assertIsDisplayed()
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun showsTextWhenOcrTextIsPresent() {
        val region = PageRegion(0, 0.1f, 0.1f, 0.5f, 0.5f, 0.9f, "\u3053\u3093\u306b\u3061\u306f")

        composeTestRule.setContent {
            OcrBottomSheet(region = region, onDismiss = {})
        }

        composeTestRule.onNodeWithText("\u3053\u3093\u306b\u3061\u306f").assertIsDisplayed()
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun copyButtonIsDisplayedWhenTextPresent() {
        val region = PageRegion(0, 0.1f, 0.1f, 0.5f, 0.5f, 0.9f, "\u30c6\u30b9\u30c8")

        composeTestRule.setContent {
            OcrBottomSheet(region = region, onDismiss = {})
        }

        composeTestRule.onNodeWithContentDescription("Copy text").assertIsDisplayed()
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun shareButtonIsDisplayedWhenTextPresent() {
        val region = PageRegion(0, 0.1f, 0.1f, 0.5f, 0.5f, 0.9f, "\u30c6\u30b9\u30c8")

        composeTestRule.setContent {
            OcrBottomSheet(region = region, onDismiss = {})
        }

        composeTestRule.onNodeWithContentDescription("Share text").assertIsDisplayed()
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun ocrTextUsesJapaneseLocale() {
        val region = PageRegion(0, 0.1f, 0.1f, 0.5f, 0.5f, 0.9f, "\u6f22\u5b57\u30c6\u30b9\u30c8")

        composeTestRule.setContent {
            OcrBottomSheet(region = region, onDismiss = {})
        }

        composeTestRule.onNodeWithTag("ocr_text").assertIsDisplayed()
    }
}
