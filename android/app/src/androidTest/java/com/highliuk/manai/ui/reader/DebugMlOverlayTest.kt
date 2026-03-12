package com.highliuk.manai.ui.reader

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.highliuk.manai.domain.model.Manga
import com.highliuk.manai.domain.model.PagePipelineState
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
}
