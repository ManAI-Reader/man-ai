package com.highliuk.manai.ui.reader

import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.platform.app.InstrumentationRegistry
import com.highliuk.manai.domain.model.PageRegion
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

class LandscapeCutoutRule : TestWatcher() {
    override fun starting(description: Description) {
        val auto = InstrumentationRegistry.getInstrumentation().uiAutomation
        auto.executeShellCommand(
            "cmd overlay enable com.android.internal.display.cutout.emulation.hole"
        ).close()
        auto.executeShellCommand("settings put system accelerometer_rotation 0").close()
        auto.executeShellCommand("settings put system user_rotation 1").close()
        Thread.sleep(1500)
    }

    override fun finished(description: Description) {
        val auto = InstrumentationRegistry.getInstrumentation().uiAutomation
        auto.executeShellCommand("settings put system user_rotation 0").close()
        auto.executeShellCommand("settings put system accelerometer_rotation 1").close()
        auto.executeShellCommand(
            "cmd overlay disable com.android.internal.display.cutout.emulation.hole"
        ).close()
    }
}

class OcrBottomSheetCutoutTest {

    private val landscapeCutoutRule = LandscapeCutoutRule()
    private val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @get:Rule
    val ruleChain: RuleChain = RuleChain
        .outerRule(landscapeCutoutRule)
        .around(composeTestRule)

    @Test
    fun ocrSheetContent_doesNotOverlap_displayCutout() {
        var cutoutLeftPx = 0
        var cutoutRightPx = 0

        composeTestRule.activityRule.scenario.onActivity { activity ->
            activity.enableEdgeToEdge()
            activity.window.attributes = activity.window.attributes.apply {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }

        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.setContent {
            val density = LocalDensity.current
            cutoutLeftPx = WindowInsets.displayCutout.getLeft(density, LayoutDirection.Ltr)
            cutoutRightPx = WindowInsets.displayCutout.getRight(density, LayoutDirection.Ltr)

            OcrBottomSheet(
                region = PageRegion(0, 0.1f, 0.1f, 0.5f, 0.5f, 0.9f, "テスト文章"),
                onDismiss = {},
            )
        }
        composeTestRule.mainClock.advanceTimeBy(1000)
        composeTestRule.mainClock.autoAdvance = true
        composeTestRule.waitForIdle()

        val hasCutout = cutoutLeftPx > 0 || cutoutRightPx > 0
        assumeTrue(
            "Device must have a lateral display cutout " +
                "(left=$cutoutLeftPx, right=$cutoutRightPx)",
            hasCutout,
        )

        val contentBounds = composeTestRule
            .onNodeWithTag("ocr_sheet_content")
            .getUnclippedBoundsInRoot()

        val contentLeftPx = with(composeTestRule.density) { contentBounds.left.toPx() }
        val contentRightPx = with(composeTestRule.density) { contentBounds.right.toPx() }
        val screenWidthPx = composeTestRule.activity.window.decorView.width.toFloat()

        if (cutoutLeftPx > 0) {
            assertTrue(
                "Content left edge (${contentLeftPx}px) must not extend into display cutout " +
                    "area (cutout left: ${cutoutLeftPx}px)",
                contentLeftPx >= cutoutLeftPx,
            )
        }

        if (cutoutRightPx > 0) {
            assertTrue(
                "Content right edge (${contentRightPx}px) must not extend into display cutout " +
                    "area (max allowed: ${screenWidthPx - cutoutRightPx}px)",
                contentRightPx <= screenWidthPx - cutoutRightPx,
            )
        }
    }
}
