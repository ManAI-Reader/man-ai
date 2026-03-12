package com.highliuk.manai.ui.reader

import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ReaderBottomBarOverlapTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun slider_doesNotOverlap_systemNavigationBar() {
        var screenHeightPx = 0f
        var navBarTopPx = 0f

        composeTestRule.activityRule.scenario.onActivity { activity ->
            activity.enableEdgeToEdge()
        }

        composeTestRule.setContent {
            val density = LocalDensity.current
            val navBarInsets = WindowInsets.navigationBars
            val navBarBottomPx = with(density) {
                navBarInsets.getBottom(density).toFloat()
            }

            Box(modifier = Modifier.fillMaxSize()) {
                screenHeightPx = with(density) {
                    composeTestRule.activity.window.decorView.height.toFloat()
                }
                navBarTopPx = screenHeightPx - navBarBottomPx

                Box(modifier = Modifier.align(Alignment.BottomCenter)) {
                    ReaderBottomBar(
                        currentPage = 0,
                        pageCount = 10,
                        onPageSelected = {}
                    )
                }
            }
        }

        composeTestRule.waitForIdle()

        val sliderBounds = composeTestRule
            .onNodeWithTag("page_slider")
            .getUnclippedBoundsInRoot()

        val sliderBottomPx = with(composeTestRule.density) {
            sliderBounds.bottom.toPx()
        }

        assertTrue(
            "Slider bottom ($sliderBottomPx) must not extend into navigation bar area (top: $navBarTopPx). " +
                "Overlap: ${sliderBottomPx - navBarTopPx}px",
            sliderBottomPx <= navBarTopPx
        )
    }
}
