package com.highliuk.manai.ui.navigation

import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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

    @Test
    fun exitImmersiveMode_resetsBehaviorToDefault() {
        composeTestRule.activityRule.scenario.onActivity { activity ->
            activity.enableEdgeToEdge()
            val insetsController = WindowCompat.getInsetsController(
                activity.window, activity.window.decorView
            )

            applyImmersiveMode(insetsController, immersive = true)
            applyImmersiveMode(insetsController, immersive = false)

            assertEquals(
                "systemBarsBehavior should be BEHAVIOR_DEFAULT after exiting immersive mode",
                WindowInsetsControllerCompat.BEHAVIOR_DEFAULT,
                insetsController.systemBarsBehavior
            )
        }
    }

    @Test
    fun exitImmersiveMode_showsSystemBars() {
        composeTestRule.activityRule.scenario.onActivity { activity ->
            activity.enableEdgeToEdge()
            val insetsController = WindowCompat.getInsetsController(
                activity.window, activity.window.decorView
            )

            applyImmersiveMode(insetsController, immersive = true)
            applyImmersiveMode(insetsController, immersive = false)
        }

        composeTestRule.waitForIdle()

        composeTestRule.activityRule.scenario.onActivity { activity ->
            val insets = ViewCompat.getRootWindowInsets(activity.window.decorView)
            assertTrue(
                "Status bar should be visible after exiting immersive mode",
                insets?.isVisible(WindowInsetsCompat.Type.statusBars()) ?: false
            )
        }
    }
}
