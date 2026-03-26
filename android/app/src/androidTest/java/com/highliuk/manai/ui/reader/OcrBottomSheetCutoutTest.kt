package com.highliuk.manai.ui.reader

import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.platform.app.InstrumentationRegistry
import com.highliuk.manai.domain.model.PageRegion
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

class LandscapeRule : TestWatcher() {
    override fun starting(description: Description) {
        val auto = InstrumentationRegistry.getInstrumentation().uiAutomation
        auto.executeShellCommand("settings put system accelerometer_rotation 0").close()
        auto.executeShellCommand("settings put system user_rotation 1").close()
        Thread.sleep(1500)
    }

    override fun finished(description: Description) {
        val auto = InstrumentationRegistry.getInstrumentation().uiAutomation
        auto.executeShellCommand("settings put system user_rotation 0").close()
        auto.executeShellCommand("settings put system accelerometer_rotation 1").close()
    }
}

private const val CUTOUT_PX = 80

class OcrBottomSheetCutoutTest {

    private val landscapeRule = LandscapeRule()
    private val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @get:Rule
    val ruleChain: RuleChain = RuleChain
        .outerRule(landscapeRule)
        .around(composeTestRule)

    @Test
    fun ocrSheetContent_doesNotOverlap_displayCutout() {
        composeTestRule.activityRule.scenario.onActivity { activity ->
            activity.enableEdgeToEdge()
            activity.window.attributes = activity.window.attributes.apply {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }

        val fakeCutout = WindowInsets(left = CUTOUT_PX, right = CUTOUT_PX)

        composeTestRule.setContent {
            OcrBottomSheet(
                region = PageRegion(0, 0.1f, 0.1f, 0.5f, 0.5f, 0.9f, "テスト文章"),
                onDismiss = {},
                cutoutInsets = fakeCutout,
            )
        }
        composeTestRule.waitForIdle()

        assertContentRespectsDisplayCutout()
    }

    private fun assertContentRespectsDisplayCutout() {
        val contentBounds = composeTestRule
            .onNodeWithTag("ocr_sheet_content")
            .getUnclippedBoundsInRoot()

        val contentLeftPx = with(composeTestRule.density) { contentBounds.left.toPx() }
        val contentRightPx = with(composeTestRule.density) { contentBounds.right.toPx() }
        val screenWidthPx = composeTestRule.activity.window.decorView.width.toFloat()

        assertTrue(
            "Content left (${contentLeftPx}px) overlaps cutout (${CUTOUT_PX}px)",
            contentLeftPx >= CUTOUT_PX,
        )
        assertTrue(
            "Content right (${contentRightPx}px) overlaps cutout " +
                "(max: ${screenWidthPx - CUTOUT_PX}px)",
            contentRightPx <= screenWidthPx - CUTOUT_PX,
        )
    }
}
