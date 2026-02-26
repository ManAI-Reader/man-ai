package com.highliuk.manai.ui.reader

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class WebtoonViewerTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun rendersAtInitialScrollPosition() {
        lateinit var lazyListState: LazyListState

        composeTestRule.setContent {
            lazyListState = rememberLazyListState(initialFirstVisibleItemIndex = 3)
            WebtoonViewer(
                lazyListState = lazyListState,
                uri = "content://test",
                pageCount = 10,
                gestureState = ReaderGestureState(),
            )
        }

        composeTestRule.waitForIdle()
        assertEquals(3, lazyListState.firstVisibleItemIndex)
    }

    @Test
    fun scrollUpdatesSharedState() {
        lateinit var lazyListState: LazyListState

        composeTestRule.setContent {
            lazyListState = rememberLazyListState()
            WebtoonViewer(
                lazyListState = lazyListState,
                uri = "content://test",
                pageCount = 10,
                gestureState = ReaderGestureState(),
            )
        }

        composeTestRule.onNodeWithTag("webtoon_viewer")
            .performTouchInput { swipeUp() }
        composeTestRule.waitForIdle()

        assertTrue(lazyListState.firstVisibleItemIndex > 0)
    }

    @Test
    fun tapTogglesBars() {
        val gestureState = ReaderGestureState()

        composeTestRule.setContent {
            WebtoonViewer(
                lazyListState = rememberLazyListState(),
                uri = "content://test",
                pageCount = 5,
                gestureState = gestureState,
            )
        }

        assertFalse(gestureState.areBarsVisible)

        composeTestRule.onNodeWithTag("webtoon_viewer").performClick()
        composeTestRule.mainClock.advanceTimeBy(500)

        assertTrue(gestureState.areBarsVisible)
    }

    @Test
    fun scrollWorksWhenZoomed() {
        val gestureState = ReaderGestureState()
        gestureState.onZoom(2f)
        lateinit var lazyListState: LazyListState

        composeTestRule.setContent {
            lazyListState = rememberLazyListState()
            WebtoonViewer(
                lazyListState = lazyListState,
                uri = "content://test",
                pageCount = 10,
                gestureState = gestureState,
            )
        }

        composeTestRule.onNodeWithTag("webtoon_viewer")
            .performTouchInput { swipeUp() }
        composeTestRule.waitForIdle()

        assertTrue(lazyListState.firstVisibleItemIndex > 0)
    }
}
