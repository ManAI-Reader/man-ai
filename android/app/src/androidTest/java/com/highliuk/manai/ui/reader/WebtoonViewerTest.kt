package com.highliuk.manai.ui.reader

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
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
import androidx.compose.ui.test.pinch
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
    fun doubleTapZoomsAroundTapPoint() {
        val gestureState = ReaderGestureState()
        lateinit var lazyListState: LazyListState

        composeTestRule.setContent {
            lazyListState = rememberLazyListState()
            WebtoonViewer(
                lazyListState = lazyListState,
                uri = "content://test",
                pageCount = 20,
                gestureState = gestureState,
            )
        }

        composeTestRule.waitForIdle()
        val viewportHeight = lazyListState.layoutInfo.viewportSize.height.toFloat()
        val pageTop = lazyListState.layoutInfo.visibleItemsInfo
            .first { it.index == 0 }.offset.toFloat()
        val pageHeight = lazyListState.layoutInfo.visibleItemsInfo
            .first { it.index == 0 }.size.toFloat()

        // Double-tap near the top of the first page. The tap point is well above
        // the viewport center, so the zoom must produce a large positive offsetY
        // to shift the content down and center on the tap point.
        composeTestRule.onNodeWithTag("webtoon_page_0")
            .performTouchInput { doubleClick(topCenter.copy(y = 10f)) }
        composeTestRule.mainClock.advanceTimeBy(500)

        assertEquals("scale should be 2x after double-tap", 2f, gestureState.scale, 0.01f)

        // With correct viewport coords: offsetY ≈ viewportCenter - (pageTop + 10)
        // With the bug (page coords): offsetY ≈ pageHeight/2 - 10 (much smaller)
        val viewportCenter = viewportHeight / 2f
        val expectedOffsetY = (viewportCenter - (pageTop + 10f))
            .coerceIn(-viewportHeight / 2f, viewportHeight / 2f)
        assertEquals(
            "offsetY should use viewport coordinates, not page coordinates",
            expectedOffsetY, gestureState.offsetY, viewportHeight * 0.15f
        )
    }

    @Test
    fun scrollWorksWhenZoomed() {
        val gestureState = ReaderGestureState()
        gestureState.onZoom(2f, Offset(200f, 400f), 400f, 800f)
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

    @Test
    fun pinchAtCornerShiftsOffsetTowardCorner() {
        val gestureState = ReaderGestureState()

        composeTestRule.setContent {
            WebtoonViewer(
                lazyListState = rememberLazyListState(),
                uri = "content://test",
                pageCount = 5,
                gestureState = gestureState,
            )
        }

        composeTestRule.onNodeWithTag("webtoon_viewer")
            .performTouchInput {
                // Pinch out (zoom in) at top-left corner
                // Fingers spread apart, centroid near (75, 75)
                pinch(
                    start0 = Offset(50f, 50f),
                    end0 = Offset(25f, 25f),
                    start1 = Offset(100f, 100f),
                    end1 = Offset(125f, 125f),
                )
            }
        composeTestRule.waitForIdle()

        assertTrue("should be zoomed after pinch", gestureState.scale > 1f)
        assertTrue(
            "offsetX should be > 0 when pinching at top-left (focal point shifts view right)",
            gestureState.offsetX > 0f
        )
        assertTrue(
            "offsetY should be > 0 when pinching at top-left (focal point shifts view down)",
            gestureState.offsetY > 0f
        )
    }
}
