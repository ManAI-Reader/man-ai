package com.highliuk.manai.ui.reader

import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pinch
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class HorizontalPagerViewerTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun pinchAtCornerShiftsOffsetTowardCorner() {
        val gestureState = ReaderGestureState()

        composeTestRule.setContent {
            val pagerState = rememberPagerState(pageCount = { 5 })
            HorizontalPagerViewer(
                pagerState = pagerState,
                uri = "content://test",
                isRtl = false,
                gestureState = gestureState,
            )
        }

        composeTestRule.onAllNodesWithTag("reader_zoom_container")[0]
            .performTouchInput {
                // Pinch out (zoom in) at top-left corner
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
