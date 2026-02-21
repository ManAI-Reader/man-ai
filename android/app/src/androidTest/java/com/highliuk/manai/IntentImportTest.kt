package com.highliuk.manai

import android.content.Intent
import android.net.Uri
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.highliuk.manai.data.local.dao.MangaDao
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import javax.inject.Inject

@HiltAndroidTest
class IntentImportTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createEmptyComposeRule()

    @Inject
    lateinit var mangaDao: MangaDao

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    @Test
    fun launchWithActionView_backFromReaderExitsApp() = runTest {
        val intent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("file:///test/intent-back.pdf"),
            ApplicationProvider.getApplicationContext(),
            MainActivity::class.java
        )

        ActivityScenario.launch<MainActivity>(intent).use { scenario ->
            composeTestRule.waitUntil(timeoutMillis = 5000) {
                composeTestRule.onAllNodesWithTag("reader_pager").fetchSemanticsNodes().isNotEmpty()
            }

            // Verify we're on the reader
            composeTestRule.onNodeWithTag("reader_pager").assertIsDisplayed()

            // Show top bar so we can tap back
            composeTestRule.onNodeWithTag("reader_pager").performClick()
            composeTestRule.waitUntil(timeoutMillis = 3000) {
                composeTestRule.onAllNodesWithContentDescription("Back").fetchSemanticsNodes().isNotEmpty()
            }

            // Tap back button
            composeTestRule.onNodeWithContentDescription("Back").performClick()
            composeTestRule.waitForIdle()
            composeTestRule.mainClock.advanceTimeBy(500)
            composeTestRule.waitForIdle()

            // Activity should be destroyed (not showing home)
            assertTrue(
                "Activity should be destroyed after back from intent-launched reader",
                scenario.state == Lifecycle.State.DESTROYED
            )
        }
    }

    @Test
    fun launchWithActionView_importsMangaAndNavigatesToReader() = runTest {
        val intent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("file:///test/intent-import.pdf"),
            ApplicationProvider.getApplicationContext(),
            MainActivity::class.java
        )

        ActivityScenario.launch<MainActivity>(intent).use { scenario ->
            composeTestRule.waitUntil(timeoutMillis = 5000) {
                composeTestRule.onAllNodesWithTag("reader_pager").fetchSemanticsNodes().isNotEmpty()
            }

            // Verify manga was imported to DB
            val allManga = mangaDao.getAll().first()
            assertEquals(1, allManga.size)
            assertEquals("intent-import", allManga[0].title)

            // Verify navigated to reader
            composeTestRule.onNodeWithTag("reader_pager").assertIsDisplayed()
        }
    }
}
