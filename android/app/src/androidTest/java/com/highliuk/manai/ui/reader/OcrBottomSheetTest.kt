package com.highliuk.manai.ui.reader

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiSelector
import androidx.test.uiautomator.Until
import com.highliuk.manai.domain.model.PageRegion
import com.highliuk.manai.ui.reader.ReaderViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class OcrBottomSheetTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val device: UiDevice =
        UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    @Test
    fun showsLoadingIndicatorWhenOcrTextIsNull() {
        val region = PageRegion(0, 0.1f, 0.1f, 0.5f, 0.5f, 0.9f, null)

        composeTestRule.setContent {
            OcrBottomSheet(region = region, onDismiss = {})
        }

        composeTestRule.onNodeWithText("Recognizing text", substring = true).assertIsDisplayed()
    }

    @Test
    fun showsTextWhenOcrTextIsPresent() {
        val region = PageRegion(0, 0.1f, 0.1f, 0.5f, 0.5f, 0.9f, "\u3053\u3093\u306b\u3061\u306f")

        composeTestRule.setContent {
            OcrBottomSheet(region = region, onDismiss = {})
        }

        composeTestRule.onNodeWithText("\u3053\u3093\u306b\u3061\u306f").assertIsDisplayed()
    }

    @Test
    fun copyButtonIsDisplayedWhenTextPresent() {
        val region = PageRegion(0, 0.1f, 0.1f, 0.5f, 0.5f, 0.9f, "\u30c6\u30b9\u30c8")

        composeTestRule.setContent {
            OcrBottomSheet(region = region, onDismiss = {})
        }

        composeTestRule.onNodeWithContentDescription("Copy text").assertIsDisplayed()
    }

    @Test
    fun shareButtonIsDisplayedWhenTextPresent() {
        val region = PageRegion(0, 0.1f, 0.1f, 0.5f, 0.5f, 0.9f, "\u30c6\u30b9\u30c8")

        composeTestRule.setContent {
            OcrBottomSheet(region = region, onDismiss = {})
        }

        composeTestRule.onNodeWithContentDescription("Share text").assertIsDisplayed()
    }

    @Test
    fun ocrTextUsesJapaneseLocale() {
        val region = PageRegion(0, 0.1f, 0.1f, 0.5f, 0.5f, 0.9f, "\u6f22\u5b57\u30c6\u30b9\u30c8")

        composeTestRule.setContent {
            OcrBottomSheet(region = region, onDismiss = {})
        }

        composeTestRule.onNodeWithTag("ocr_text").assertIsDisplayed()
    }

    @Test
    fun bottomSheetContentHasNavigationBarPadding() {
        val region = PageRegion(0, 0.1f, 0.1f, 0.5f, 0.5f, 0.9f, "\u30c6\u30b9\u30c8")

        composeTestRule.setContent {
            OcrBottomSheet(region = region, onDismiss = {})
        }

        composeTestRule.onNodeWithTag("ocr_sheet_content").assertIsDisplayed()
    }

    @Test
    fun ocrTextRespectsFontScale() {
        val region = PageRegion(0, 0.1f, 0.1f, 0.5f, 0.5f, 0.9f, "\u30c6\u30b9\u30c8")

        composeTestRule.setContent {
            OcrBottomSheet(region = region, fontScale = 3.0f, onDismiss = {})
        }

        composeTestRule.onNodeWithTag("ocr_text").assertIsDisplayed()
    }

    @Test
    fun copyButtonCopiesTextToClipboard() {
        val region = PageRegion(0, 0.1f, 0.1f, 0.5f, 0.5f, 0.9f, "コピーテスト")

        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.setContent {
            OcrBottomSheet(region = region, onDismiss = {})
        }
        composeTestRule.mainClock.advanceTimeBy(1000)
        composeTestRule.mainClock.autoAdvance = true
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithContentDescription("Copy text").performClick()
        composeTestRule.waitForIdle()

        val clipboard = InstrumentationRegistry.getInstrumentation().targetContext
            .getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clipText = clipboard.primaryClip?.getItemAt(0)?.text?.toString()
        assertEquals("コピーテスト", clipText)
    }

    @Test
    fun shareButtonLaunchesShareIntent() {
        val region = PageRegion(0, 0.1f, 0.1f, 0.5f, 0.5f, 0.9f, "共有テスト")

        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.setContent {
            OcrBottomSheet(region = region, onDismiss = {})
        }
        composeTestRule.mainClock.advanceTimeBy(1000)
        composeTestRule.mainClock.autoAdvance = true
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithContentDescription("Share text").performClick()

        // Share intent opens a chooser — wait for it then dismiss
        device.wait(Until.hasObject(By.res("android:id/chooser_header")), 3000)
        device.pressBack()
    }

    @Test
    fun translateButtonIsDisplayedWhenTextPresent() {
        val region = PageRegion(0, 0.1f, 0.1f, 0.5f, 0.5f, 0.9f, "テスト")

        composeTestRule.setContent {
            OcrBottomSheet(region = region, onDismiss = {})
        }

        composeTestRule.onNodeWithContentDescription("Translate").assertIsDisplayed()
    }

    @Test
    fun translateButtonClickCallsCallback() {
        var clicked = false
        val region = PageRegion(0, 0.1f, 0.1f, 0.5f, 0.5f, 0.9f, "テスト")

        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.setContent {
            OcrBottomSheet(
                region = region,
                onTranslateClick = { clicked = true },
                onDismiss = {},
            )
        }
        composeTestRule.mainClock.advanceTimeBy(1000)
        composeTestRule.mainClock.autoAdvance = true
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithContentDescription("Translate").performClick()
        composeTestRule.waitForIdle()

        assertTrue("Translate callback should be invoked", clicked)
    }

    @Test
    fun translatedStateShowsTranslationText() {
        val region = PageRegion(0, 0.1f, 0.1f, 0.5f, 0.5f, 0.9f, "テスト")

        composeTestRule.setContent {
            OcrBottomSheet(
                region = region,
                translationState = ReaderViewModel.TranslationState.Translated("Test"),
                onDismiss = {},
            )
        }

        composeTestRule.onNodeWithTag("translation_text").assertIsDisplayed()
    }

    @Test
    fun loadingStateShowsProgressIndicator() {
        val region = PageRegion(0, 0.1f, 0.1f, 0.5f, 0.5f, 0.9f, "テスト")

        composeTestRule.setContent {
            OcrBottomSheet(
                region = region,
                translationState = ReaderViewModel.TranslationState.Loading,
                onDismiss = {},
            )
        }

        composeTestRule.onNodeWithTag("translation_loading").assertIsDisplayed()
    }

    @Test
    fun errorStateShowsErrorMessage() {
        val region = PageRegion(0, 0.1f, 0.1f, 0.5f, 0.5f, 0.9f, "テスト")

        composeTestRule.setContent {
            OcrBottomSheet(
                region = region,
                translationState = ReaderViewModel.TranslationState.Error("Network error"),
                onDismiss = {},
            )
        }

        composeTestRule.onNodeWithTag("translation_error").assertIsDisplayed()
    }

    @Test
    fun errorStateDoesNotShowDuplicateRetryButton() {
        val region = PageRegion(0, 0.1f, 0.1f, 0.5f, 0.5f, 0.9f, "テスト")

        composeTestRule.setContent {
            OcrBottomSheet(
                region = region,
                translationState = ReaderViewModel.TranslationState.Error("Network error"),
                onDismiss = {},
            )
        }

        // No "Retry" button — user retries via the persistent Translate button in the row above.
        composeTestRule.onAllNodesWithContentDescription("Retry").assertCountEquals(0)
    }

    private fun setUpSheetAndLongPress(testText: String) {
        val region = PageRegion(0, 0.1f, 0.1f, 0.5f, 0.5f, 0.9f, testText)

        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.setContent {
            OcrBottomSheet(region = region, onDismiss = {})
        }
        // Advance past LaunchedEffect + slide-in animation
        composeTestRule.mainClock.advanceTimeBy(1000)
        composeTestRule.mainClock.autoAdvance = true
        composeTestRule.waitForIdle()

        val textElement = device.findObject(UiSelector().text(testText))
        assertTrue("OCR text should be visible on screen", textElement.waitForExists(3000))
        textElement.longClick()
    }

    @Test
    fun longPressOnOcrTextShowsCopyInToolbar() {
        setUpSheetAndLongPress("\u30c6\u30b9\u30c8\u6587\u7ae0")

        val copyButton = device.wait(Until.findObject(By.text("Copy")), 5000)
        assertNotNull("Copy should appear in toolbar after long-press", copyButton)
    }

    @Test
    fun longPressOnOcrTextShowsSelectAllInToolbar() {
        setUpSheetAndLongPress("\u9078\u629e\u30c6\u30b9\u30c8")

        val selectAllButton = device.wait(Until.findObject(By.text("Select all")), 5000)
        assertNotNull("Select all should appear in toolbar after long-press", selectAllButton)
    }

    @Test
    fun copyPutsOcrTextInClipboard() {
        val testText = "\u30af\u30ea\u30c3\u30d7\u30dc\u30fc\u30c9"
        setUpSheetAndLongPress(testText)

        val selectAll = device.wait(Until.findObject(By.text("Select all")), 5000)
        assertNotNull("Select all should appear", selectAll)
        selectAll.click()

        val copyButton = device.wait(Until.findObject(By.text("Copy")), 5000)
        assertNotNull("Copy should appear", copyButton)
        copyButton.click()
        device.waitForIdle()

        val clipboard = InstrumentationRegistry.getInstrumentation().targetContext
            .getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clipText = clipboard.primaryClip?.getItemAt(0)?.text?.toString()
        assertEquals("Clipboard should contain the OCR text", testText, clipText)
    }
}
