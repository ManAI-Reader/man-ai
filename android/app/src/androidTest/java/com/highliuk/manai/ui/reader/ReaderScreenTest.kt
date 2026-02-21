package com.highliuk.manai.ui.reader

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.doubleClick
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import com.highliuk.manai.domain.model.Manga
import com.highliuk.manai.domain.model.ReadingMode
import androidx.compose.runtime.mutableStateOf
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
        onPageChanged: (Int) -> Unit = { lastPage = it },
        onImmersiveModeChange: (Boolean) -> Unit = {}
    ) {
        lastPage = 0
        composeTestRule.setContent {
            ReaderScreen(
                manga = testManga,
                currentPage = 0,
                onPageChanged = onPageChanged,
                onBack = onBack,
                onSettingsClick = onSettingsClick,
                onImmersiveModeChange = onImmersiveModeChange
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

    @Test
    fun swipeRight_changesPage_inRtlMode() {
        lastPage = 0
        composeTestRule.setContent {
            ReaderScreen(
                manga = testManga,
                currentPage = 0,
                readingMode = ReadingMode.RTL,
                onPageChanged = { lastPage = it },
                onBack = {},
                onSettingsClick = {}
            )
        }
        composeTestRule.onNodeWithTag("reader_pager")
            .performTouchInput { swipeRight() }
        composeTestRule.waitForIdle()
        assertEquals(1, lastPage)
    }

    @Test
    fun goToPage_clampsAboveMax_toLastPage() {
        setUpReaderScreen()
        // Show bars
        composeTestRule.onNodeWithTag("reader_pager").performClick()
        advancePastDoubleTapTimeout()
        // Tap page indicator to open dialog
        composeTestRule.onNodeWithTag("page_indicator").performClick()
        // Enter value above pageCount (10)
        composeTestRule.onNodeWithTag("go_to_page_input").performTextInput("99")
        composeTestRule.onNodeWithText("OK").performClick()
        composeTestRule.waitForIdle()
        assertEquals(9, lastPage)
    }

    @Test
    fun goToPage_clampsBelowMin_toFirstPage() {
        setUpReaderScreen()
        // Show bars
        composeTestRule.onNodeWithTag("reader_pager").performClick()
        advancePastDoubleTapTimeout()
        // Tap page indicator to open dialog
        composeTestRule.onNodeWithTag("page_indicator").performClick()
        // Enter 0 (below minimum of 1)
        composeTestRule.onNodeWithTag("go_to_page_input").performTextInput("0")
        composeTestRule.onNodeWithText("OK").performClick()
        composeTestRule.waitForIdle()
        assertEquals(0, lastPage)
    }

    @Test
    fun immersiveMode_disabledOnDispose() {
        val immersiveStates = mutableListOf<Boolean>()
        val showReader = mutableStateOf(true)
        composeTestRule.setContent {
            if (showReader.value) {
                ReaderScreen(
                    manga = testManga,
                    currentPage = 0,
                    onPageChanged = {},
                    onBack = {},
                    onSettingsClick = {},
                    onImmersiveModeChange = { immersiveStates.add(it) }
                )
            }
        }
        composeTestRule.waitForIdle()
        // At this point immersive was enabled (true) from LaunchedEffect
        // Now remove ReaderScreen from composition
        showReader.value = false
        composeTestRule.waitForIdle()
        // Last call should be false (restore status bar)
        assertEquals(false, immersiveStates.last())
    }

    @Test
    fun immersiveMode_reenabledAfterSecondTap() {
        var lastImmersiveState: Boolean? = null
        setUpReaderScreen(onImmersiveModeChange = { lastImmersiveState = it })
        // tap 1: show bars
        composeTestRule.onNodeWithTag("reader_pager").performClick()
        advancePastDoubleTapTimeout()
        // tap 2: hide bars
        composeTestRule.onNodeWithTag("reader_pager").performClick()
        advancePastDoubleTapTimeout()
        composeTestRule.waitForIdle()
        assertEquals(true, lastImmersiveState)
    }

    @Test
    fun immersiveMode_disabledAfterTap_whenBarsShown() {
        var lastImmersiveState: Boolean? = null
        setUpReaderScreen(onImmersiveModeChange = { lastImmersiveState = it })
        composeTestRule.onNodeWithTag("reader_pager").performClick()
        advancePastDoubleTapTimeout()
        composeTestRule.waitForIdle()
        assertEquals(false, lastImmersiveState)
    }

    @Test
    fun immersiveMode_enabledByDefault_whenBarsHidden() {
        var lastImmersiveState: Boolean? = null
        setUpReaderScreen(onImmersiveModeChange = { lastImmersiveState = it })
        composeTestRule.waitForIdle()
        assertEquals(true, lastImmersiveState)
    }

    @Test
    fun swipeLeft_doesNotAdvancePage_inRtlMode() {
        lastPage = 0
        composeTestRule.setContent {
            ReaderScreen(
                manga = testManga,
                currentPage = 0,
                readingMode = ReadingMode.RTL,
                onPageChanged = { lastPage = it },
                onBack = {},
                onSettingsClick = {}
            )
        }
        composeTestRule.onNodeWithTag("reader_pager")
            .performTouchInput { swipeLeft() }
        composeTestRule.waitForIdle()
        assertEquals(0, lastPage)
    }
}
