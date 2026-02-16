package com.highliuk.manai.ui.reader

import androidx.compose.ui.test.assertIsDisplayed
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
        composeTestRule.onNodeWithText("One Piece").assertIsDisplayed()
    }

    @Test
    fun doubleTapOnPager_hidesTopBarAgain() {
        setUpReaderScreen()
        // First tap: show
        composeTestRule.onNodeWithTag("reader_pager").performClick()
        composeTestRule.onNodeWithText("One Piece").assertIsDisplayed()
        // Second tap: hide
        composeTestRule.onNodeWithTag("reader_pager").performClick()
        composeTestRule.onNodeWithText("One Piece").assertDoesNotExist()
    }

    @Test
    fun backButton_callsOnBack_whenTopBarVisible() {
        var backCalled = false
        setUpReaderScreen(onBack = { backCalled = true })

        // Show top bar first
        composeTestRule.onNodeWithTag("reader_pager").performClick()
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
        composeTestRule.onNodeWithContentDescription("Reader settings").performClick()
        assert(settingsCalled)
    }
}
