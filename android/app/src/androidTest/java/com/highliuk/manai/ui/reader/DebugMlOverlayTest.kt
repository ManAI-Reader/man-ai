package com.highliuk.manai.ui.reader

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.highliuk.manai.domain.model.BalloonPipelineStatus
import com.highliuk.manai.domain.model.Manga
import com.highliuk.manai.domain.model.PagePipelineState
import com.highliuk.manai.domain.model.PageRegion
import com.highliuk.manai.domain.model.PipelineStatus
import org.junit.Rule
import org.junit.Test

class DebugMlOverlayTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun debugMlOverlay_renderedWhenDebugPipelineStatesProvided() {
        composeTestRule.setContent {
            ReaderScreen(
                manga = Manga(id = 1, uri = "content://test", title = "Test", pageCount = 1),
                currentPage = 0,
                onPageChanged = {},
                onBack = {},
                onSettingsClick = {},
                debugPipelineStates = mapOf(0 to PagePipelineState(0, PipelineStatus.Done)),
            )
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("debug_ml_overlay").assertIsDisplayed()
    }

    @Test
    fun debugMlOverlay_isInsideZoomTransform() {
        composeTestRule.setContent {
            ReaderScreen(
                manga = Manga(id = 1, uri = "content://test", title = "Test", pageCount = 1),
                currentPage = 0,
                onPageChanged = {},
                onBack = {},
                onSettingsClick = {},
                debugPipelineStates = mapOf(0 to PagePipelineState(0, PipelineStatus.Done)),
            )
        }

        composeTestRule.onNodeWithTag("debug_ml_overlay")
            .assert(hasAnyAncestor(SemanticsMatcher.keyIsDefined(ZoomScaleKey)))
    }

    @Test
    fun debugMlOverlay_rendersRegionsWithBalloonStatuses() {
        val regions = listOf(
            PageRegion(0, 0.1f, 0.1f, 0.4f, 0.4f, 0.9f, null),
            PageRegion(1, 0.5f, 0.5f, 0.9f, 0.9f, 0.8f, null),
            PageRegion(2, 0.2f, 0.6f, 0.5f, 0.8f, 0.7f, null),
        )
        val pageState = PagePipelineState(
            pageIndex = 0,
            pageStatus = PipelineStatus.Processing,
            balloonStatuses = mapOf(
                0 to BalloonPipelineStatus.OcrQueued(position = 1),
                1 to BalloonPipelineStatus.OcrDone,
                2 to BalloonPipelineStatus.OcrProcessing,
            ),
        )

        composeTestRule.setContent {
            DebugMlOverlay(
                pageState = pageState,
                regions = regions,
                bitmapWidth = 800,
                bitmapHeight = 1200,
                modifier = Modifier.fillMaxSize(),
            )
        }

        composeTestRule.onNodeWithTag("debug_ml_overlay").assertIsDisplayed()
    }

    @Test
    fun debugMlOverlay_rendersQueuedBalloonWithPosition() {
        val regions = listOf(
            PageRegion(0, 0.2f, 0.2f, 0.8f, 0.8f, 0.95f, null),
        )
        val pageState = PagePipelineState(
            pageIndex = 0,
            pageStatus = PipelineStatus.Done,
            balloonStatuses = mapOf(
                0 to BalloonPipelineStatus.OcrQueued(position = 3),
            ),
        )

        composeTestRule.setContent {
            DebugMlOverlay(
                pageState = pageState,
                regions = regions,
                bitmapWidth = 1000,
                bitmapHeight = 1500,
                modifier = Modifier.fillMaxSize(),
            )
        }

        composeTestRule.onNodeWithTag("debug_ml_overlay").assertIsDisplayed()
    }

    @Test
    fun debugMlOverlay_handlesRegionWithoutBalloonStatus() {
        val regions = listOf(
            PageRegion(0, 0.1f, 0.1f, 0.5f, 0.5f, 0.9f, null),
        )
        val pageState = PagePipelineState(
            pageIndex = 0,
            pageStatus = PipelineStatus.Done,
            balloonStatuses = emptyMap(),
        )

        composeTestRule.setContent {
            DebugMlOverlay(
                pageState = pageState,
                regions = regions,
                bitmapWidth = 500,
                bitmapHeight = 700,
                modifier = Modifier.fillMaxSize(),
            )
        }

        composeTestRule.onNodeWithTag("debug_ml_overlay").assertIsDisplayed()
    }

    @Test
    fun debugMlOverlay_handlesErrorAndCacheHitStatuses() {
        val regions = listOf(
            PageRegion(0, 0.1f, 0.1f, 0.3f, 0.3f, 0.9f, null),
            PageRegion(1, 0.5f, 0.5f, 0.8f, 0.8f, 0.85f, null),
        )
        val pageState = PagePipelineState(
            pageIndex = 0,
            pageStatus = PipelineStatus.Done,
            balloonStatuses = mapOf(
                0 to BalloonPipelineStatus.OcrError("timeout"),
                1 to BalloonPipelineStatus.OcrCacheHit,
            ),
        )

        composeTestRule.setContent {
            DebugMlOverlay(
                pageState = pageState,
                regions = regions,
                bitmapWidth = 600,
                bitmapHeight = 900,
                modifier = Modifier.fillMaxSize(),
            )
        }

        composeTestRule.onNodeWithTag("debug_ml_overlay").assertIsDisplayed()
    }
}
