package com.highliuk.manai.ui.reader

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.remember
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.doubleClick
import androidx.compose.ui.test.swipeUp
import com.highliuk.manai.domain.model.PagePipelineState
import com.highliuk.manai.domain.model.PageRegion
import com.highliuk.manai.domain.model.PipelineStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
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
    fun doubleTapSetsNonZeroOffsetY() {
        val gestureState = ReaderGestureState()

        composeTestRule.setContent {
            WebtoonViewer(
                lazyListState = rememberLazyListState(),
                uri = "content://test",
                pageCount = 5,
                gestureState = gestureState,
            )
        }

        // Double-tap off-center to trigger zoom with Y offset
        composeTestRule.onNodeWithTag("webtoon_viewer")
            .performTouchInput { doubleClick(center.copy(y = center.y * 0.5f)) }
        composeTestRule.mainClock.advanceTimeBy(500)

        assertTrue("scale should be zoomed in", gestureState.scale > 1f)
        assertNotEquals("offsetY should be non-zero after off-center double-tap", 0f, gestureState.offsetY)
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

    @Test
    fun tappingRegionInvokesCallback() {
        val region = PageRegion(0, 0.0f, 0.0f, 1.0f, 1.0f, 0.9f, "text")
        var tappedRegion: PageRegion? = null

        composeTestRule.setContent {
            val lazyListState = rememberLazyListState()
            val gestureState = remember { ReaderGestureState() }
            WebtoonViewer(
                lazyListState = lazyListState,
                uri = "content://test",
                pageCount = 1,
                gestureState = gestureState,
                visiblePagesRegions = mapOf(0 to listOf(region)),
                onRegionTapped = { tappedRegion = it },
            )
        }

        composeTestRule.onNodeWithTag("webtoon_page_0")
            .performClick()

        composeTestRule.mainClock.advanceTimeBy(500)
        composeTestRule.waitForIdle()
        assertEquals(region, tappedRegion)
    }

    @Test
    fun debugOverlayMatchesPageBounds() {
        composeTestRule.setContent {
            val lazyListState = rememberLazyListState()
            val gestureState = remember { ReaderGestureState() }
            WebtoonViewer(
                lazyListState = lazyListState,
                uri = "content://test",
                pageCount = 1,
                gestureState = gestureState,
                debugPipelineStates = mapOf(
                    0 to PagePipelineState(
                        pageIndex = 0,
                        pageStatus = PipelineStatus.Done,
                        balloonStatuses = emptyMap(),
                    ),
                ),
            )
        }

        val pageBounds = composeTestRule.onNodeWithTag("webtoon_page_0")
            .getBoundsInRoot()
        val overlayBounds = composeTestRule.onAllNodesWithTag("debug_ml_overlay")[0]
            .getBoundsInRoot()

        val pageHeight = (pageBounds.bottom - pageBounds.top).value
        val overlayHeight = (overlayBounds.bottom - overlayBounds.top).value
        assertEquals(pageHeight, overlayHeight, 1f)
        assertEquals(pageBounds.top.value, overlayBounds.top.value, 1f)
    }

    @Test
    fun debugOverlayHasNonZeroHeight() {
        composeTestRule.setContent {
            val lazyListState = rememberLazyListState()
            val gestureState = remember { ReaderGestureState() }
            WebtoonViewer(
                lazyListState = lazyListState,
                uri = "content://test",
                pageCount = 1,
                gestureState = gestureState,
                debugPipelineStates = mapOf(
                    0 to PagePipelineState(
                        pageIndex = 0,
                        pageStatus = PipelineStatus.Done,
                        balloonStatuses = emptyMap(),
                    ),
                ),
            )
        }

        val overlayBounds = composeTestRule.onAllNodesWithTag("debug_ml_overlay")[0]
            .getBoundsInRoot()
        val overlayHeight = (overlayBounds.bottom - overlayBounds.top).value
        assertTrue("overlay height should be > 0 so page tint is visible", overlayHeight > 0f)
    }

    @Test
    fun debugOverlayRenderedPerPageInWebtoon() {
        composeTestRule.setContent {
            val lazyListState = rememberLazyListState()
            val gestureState = remember { ReaderGestureState() }
            WebtoonViewer(
                lazyListState = lazyListState,
                uri = "content://test",
                pageCount = 2,
                gestureState = gestureState,
                debugPipelineStates = mapOf(
                    0 to PagePipelineState(
                        pageIndex = 0,
                        pageStatus = PipelineStatus.Done,
                        balloonStatuses = emptyMap(),
                    ),
                    1 to PagePipelineState(
                        pageIndex = 1,
                        pageStatus = PipelineStatus.Processing,
                        balloonStatuses = emptyMap(),
                    ),
                ),
            )
        }

        composeTestRule.onAllNodesWithTag("debug_ml_overlay")
            .assertCountEquals(2)
    }
}
