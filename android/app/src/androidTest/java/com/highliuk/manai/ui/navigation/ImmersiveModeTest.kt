package com.highliuk.manai.ui.navigation

import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test

class ImmersiveModeTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun immersiveMode_hidesNavigationBar() {
        composeTestRule.activityRule.scenario.onActivity { activity ->
            activity.enableEdgeToEdge()
            val insetsController = WindowCompat.getInsetsController(
                activity.window, activity.window.decorView
            )

            applyImmersiveMode(insetsController, immersive = true)
        }

        composeTestRule.waitForIdle()

        composeTestRule.activityRule.scenario.onActivity { activity ->
            val insets = ViewCompat.getRootWindowInsets(activity.window.decorView)
            assertFalse(
                "Navigation bar should be hidden in immersive mode",
                insets?.isVisible(WindowInsetsCompat.Type.navigationBars()) ?: true
            )
        }
    }
}
