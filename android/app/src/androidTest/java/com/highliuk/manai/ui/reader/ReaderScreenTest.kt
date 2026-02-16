package com.highliuk.manai.ui.reader

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.doubleClick
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import com.highliuk.manai.domain.model.Manga
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ReaderScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val testManga = Manga(id = 1, uri = "content://test", title = "One Piece", pageCount = 10)

    /**
     * When onDoubleTap is registered, Compose delays onTap by doubleTapTimeoutMillis (~300ms).
     * Advance the test clock past that timeout so onTap fires before assertions.
     */
    private fun advancePastDoubleTapTimeout() {
        composeTestRule.mainClock.advanceTimeBy(500)
    }

    private var lastPage = 0

    private fun setUpReaderScreen(
        onBack: () -> Unit = {},
        onSettingsClick: () -> Unit = {},
        onPageChanged: (Int) -> Unit = { lastPage = it }
    ) {
        lastPage = 0
        composeTestRule.setContent {
            ReaderScreen(
                manga = testManga,
                currentPage = 0,
                onPageChanged = onPageChanged,
                onBack = onBack,
                onSettingsClick = onSettingsClick
            )
        }
    }

    @Test
    fun topBar_isHiddenByDefault() {
        setUpReaderScreen()
        composeTestRule.onNodeWithText("One Piece").assertDoesNotExist()
    }

    @Test
    fun tapOnPager_showsTopBar() {
        setUpReaderScreen()
        composeTestRule.onNodeWithTag("reader_pager").performClick()
        advancePastDoubleTapTimeout()
        composeTestRule.onNodeWithText("One Piece").assertIsDisplayed()
    }

    @Test
    fun doubleTapOnPager_hidesTopBarAgain() {
        setUpReaderScreen()
        // First tap: show
        composeTestRule.onNodeWithTag("reader_pager").performClick()
        advancePastDoubleTapTimeout()
        composeTestRule.onNodeWithText("One Piece").assertIsDisplayed()
        // Second tap: hide
        composeTestRule.onNodeWithTag("reader_pager").performClick()
        advancePastDoubleTapTimeout()
        composeTestRule.onNodeWithText("One Piece").assertDoesNotExist()
    }

    @Test
    fun backButton_callsOnBack_whenTopBarVisible() {
        var backCalled = false
        setUpReaderScreen(onBack = { backCalled = true })

        // Show top bar first
        composeTestRule.onNodeWithTag("reader_pager").performClick()
        advancePastDoubleTapTimeout()
        composeTestRule.onNodeWithContentDescription("Back").performClick()
        assert(backCalled)
    }

    @Test
    fun swipeLeft_changesPage_whenNotZoomed() {
        setUpReaderScreen()
        composeTestRule.onNodeWithTag("reader_pager")
            .performTouchInput { swipeLeft() }
        composeTestRule.waitForIdle()
        assertEquals(1, lastPage)
    }

    @Test
    fun pdfPage_hasZoomContainer() {
        setUpReaderScreen()
        composeTestRule.onNodeWithTag("reader_zoom_container").assertIsDisplayed()
    }

    @Test
    fun settingsButton_callsOnSettingsClick_whenTopBarVisible() {
        var settingsCalled = false
        setUpReaderScreen(onSettingsClick = { settingsCalled = true })

        // Show top bar first
        composeTestRule.onNodeWithTag("reader_pager").performClick()
        advancePastDoubleTapTimeout()
        composeTestRule.onNodeWithContentDescription("Reader settings").performClick()
        assert(settingsCalled)
    }

    @Test
    fun bottomBar_isHiddenByDefault() {
        setUpReaderScreen()
        composeTestRule.onNodeWithText("1 / 10").assertDoesNotExist()
    }

    @Test
    fun tapOnPager_showsBottomBar() {
        setUpReaderScreen()
        composeTestRule.onNodeWithTag("reader_pager").performClick()
        advancePastDoubleTapTimeout()
        composeTestRule.onNodeWithText("1 / 10").assertIsDisplayed()
    }

    @Test
    fun doubleTapOnPager_hidesBottomBarAgain() {
        setUpReaderScreen()
        composeTestRule.onNodeWithTag("reader_pager").performClick()
        advancePastDoubleTapTimeout()
        composeTestRule.onNodeWithText("1 / 10").assertIsDisplayed()
        composeTestRule.onNodeWithTag("reader_pager").performClick()
        advancePastDoubleTapTimeout()
        composeTestRule.onNodeWithText("1 / 10").assertDoesNotExist()
    }

    @Test
    fun bottomBar_showsCorrectPageOnFirstPage() {
        setUpReaderScreen()
        composeTestRule.onNodeWithTag("reader_pager").performClick()
        advancePastDoubleTapTimeout()
        composeTestRule.onNodeWithText("1 / 10").assertIsDisplayed()
    }

    @Test
    fun doubleTap_blocksSwipe_becauseZoomed() {
        setUpReaderScreen()
        composeTestRule.onNodeWithTag("reader_zoom_container")
            .performTouchInput { doubleClick() }
        composeTestRule.waitForIdle()
        // Now zoomed — swipe should be blocked
        composeTestRule.onNodeWithTag("reader_pager")
            .performTouchInput { swipeLeft() }
        composeTestRule.waitForIdle()
        assertEquals(0, lastPage)
    }

    @Test
    fun doubleTapTwice_allowsSwipe_becauseBackTo1x() {
        setUpReaderScreen()
        // First double-tap: zoom in
        composeTestRule.onNodeWithTag("reader_zoom_container")
            .performTouchInput { doubleClick() }
        composeTestRule.waitForIdle()
        // Second double-tap: zoom out
        composeTestRule.onNodeWithTag("reader_zoom_container")
            .performTouchInput { doubleClick() }
        composeTestRule.waitForIdle()
        // Back to 1x — swipe should work
        composeTestRule.onNodeWithTag("reader_pager")
            .performTouchInput { swipeLeft() }
        composeTestRule.waitForIdle()
        assertEquals(1, lastPage)
    }
}
