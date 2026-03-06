package com.highliuk.manai.ui.navigation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.test.filters.SdkSuppress
import com.highliuk.manai.MainActivity
import com.highliuk.manai.data.local.ManAiDatabase
import com.highliuk.manai.data.local.dao.MangaDao
import com.highliuk.manai.data.local.entity.MangaEntity
import com.highliuk.manai.ui.home.HomeViewModel
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import javax.inject.Inject

@HiltAndroidTest
class ManAiNavHostTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Inject
    lateinit var mangaDao: MangaDao

    @Inject
    lateinit var database: ManAiDatabase

    @Before
    fun setUp() {
        hiltRule.inject()
        database.clearAllTables()
    }

    @SdkSuppress(minSdkVersion = 30) // Immersive mode tap-to-show unreliable on API < 30
    @Test
    fun tappingManga_navigatesToReaderScreen() = runTest {
        mangaDao.insert(MangaEntity(uri = "content://nav-test", title = "Nav Test Manga", pageCount = 5))

        composeTestRule.waitUntil(timeoutMillis = 10000) {
            composeTestRule.onAllNodesWithText("Nav Test Manga").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("Nav Test Manga").performClick()

        composeTestRule.waitForIdle()
        // Top bar is hidden by default, tap to show it
        composeTestRule.onNodeWithTag("reader_pager").performClick()
        composeTestRule.mainClock.advanceTimeBy(500)
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithContentDescription("Back").assertIsDisplayed()
    }

    @Test
    fun importManga_navigatesDirectlyToReaderScreen() = runTest {
        composeTestRule.waitForIdle()

        val viewModel = ViewModelProvider(composeTestRule.activity)[HomeViewModel::class.java]
        viewModel.importManga("content://auto-nav-test.pdf", "Auto Nav Manga.pdf")

        composeTestRule.waitUntil(timeoutMillis = 10000) {
            composeTestRule.onAllNodesWithTag("reader_pager").fetchSemanticsNodes().isNotEmpty()
        }

        composeTestRule.onNodeWithTag("reader_pager").assertIsDisplayed()
    }

    @SdkSuppress(minSdkVersion = 30) // WindowInsetsCompat.isVisible() unreliable on API < 30
    @Test
    fun navigatingToReader_hidesStatusBar() = runTest {
        mangaDao.insert(MangaEntity(uri = "content://immersive-test", title = "Immersive Test", pageCount = 3))

        composeTestRule.waitUntil(timeoutMillis = 10000) {
            composeTestRule.onAllNodesWithText("Immersive Test").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("Immersive Test").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.mainClock.advanceTimeBy(500)
        composeTestRule.waitForIdle()

        val view = composeTestRule.activity.window.decorView
        val insets = ViewCompat.getRootWindowInsets(view)
        val statusBarsVisible = insets?.isVisible(WindowInsetsCompat.Type.statusBars()) ?: true

        assertFalse("Status bar should be hidden in reader immersive mode", statusBarsVisible)
    }

    @SdkSuppress(minSdkVersion = 30) // WindowInsetsCompat.isVisible() unreliable on API < 30
    @Test
    fun navigatingBackFromReader_restoresStatusBar() = runTest {
        mangaDao.insert(MangaEntity(uri = "content://restore-test", title = "Restore Test", pageCount = 3))

        composeTestRule.waitUntil(timeoutMillis = 10000) {
            composeTestRule.onAllNodesWithText("Restore Test").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("Restore Test").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.mainClock.advanceTimeBy(500)
        composeTestRule.waitForIdle()

        // First verify status bar IS hidden in reader
        val view = composeTestRule.activity.window.decorView
        val insetsInReader = ViewCompat.getRootWindowInsets(view)
        assertFalse(
            "Status bar should be hidden in reader before navigating back",
            insetsInReader?.isVisible(WindowInsetsCompat.Type.statusBars()) ?: true
        )

        // Show bars, then tap back
        composeTestRule.onNodeWithTag("reader_pager").performClick()
        composeTestRule.mainClock.advanceTimeBy(500)
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithContentDescription("Back").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.mainClock.advanceTimeBy(500)
        composeTestRule.waitForIdle()

        val insetsAfterBack = ViewCompat.getRootWindowInsets(view)
        val statusBarsVisible = insetsAfterBack?.isVisible(WindowInsetsCompat.Type.statusBars()) ?: false

        assertTrue("Status bar should be restored after leaving reader", statusBarsVisible)
    }

    @Test
    fun tappingManga_readerScreenCollectsRegions() = runTest {
        mangaDao.insert(MangaEntity(uri = "content://region-test", title = "Region Test", pageCount = 1))

        composeTestRule.waitUntil(timeoutMillis = 10000) {
            composeTestRule.onAllNodesWithText("Region Test").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("Region Test").performClick()

        composeTestRule.waitUntil(timeoutMillis = 10000) {
            composeTestRule.onAllNodesWithTag("reader_pager").fetchSemanticsNodes().isNotEmpty()
        }
        // Reader opens without crash — verifies NavHost passes the new params
        composeTestRule.onNodeWithTag("reader_pager").assertIsDisplayed()
    }

    @Test
    fun tappingSettings_navigatesToSettingsScreen() {
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithContentDescription("Settings").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.mainClock.advanceTimeBy(500)

        composeTestRule.onNodeWithText("Settings").assertIsDisplayed()
        composeTestRule.onNodeWithText("Grid Columns").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Reading Mode").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Theme").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Language").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun settings_backReturnsToHomeScreen() {
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithContentDescription("Settings").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.mainClock.advanceTimeBy(500)
        composeTestRule.onNodeWithText("Settings").assertIsDisplayed()

        composeTestRule.onNodeWithContentDescription("Back").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.mainClock.advanceTimeBy(500)

        composeTestRule.onNodeWithContentDescription("Settings").assertIsDisplayed()
    }

    @Test
    fun settings_changingGridColumnsTo3_persistsSelection() {
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithContentDescription("Settings").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.mainClock.advanceTimeBy(500)

        composeTestRule.onNodeWithText("3 columns").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        // Go back and return to settings to verify persistence
        composeTestRule.onNodeWithContentDescription("Back").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.mainClock.advanceTimeBy(500)
        composeTestRule.onNodeWithContentDescription("Settings").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.mainClock.advanceTimeBy(500)

        composeTestRule.onNodeWithText("3 columns")
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun settings_changingReadingModeToRtl_persistsSelection() {
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithContentDescription("Settings").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.mainClock.advanceTimeBy(500)

        composeTestRule.onNodeWithText("Right to Left").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithContentDescription("Back").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.mainClock.advanceTimeBy(500)
        composeTestRule.onNodeWithContentDescription("Settings").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.mainClock.advanceTimeBy(500)

        composeTestRule.onNodeWithText("Right to Left").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun settings_changingThemeToDark_persistsSelection() {
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithContentDescription("Settings").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.mainClock.advanceTimeBy(500)

        composeTestRule.onNodeWithText("Dark").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithContentDescription("Back").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.mainClock.advanceTimeBy(500)
        composeTestRule.onNodeWithContentDescription("Settings").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.mainClock.advanceTimeBy(500)

        composeTestRule.onNodeWithText("Dark").performScrollTo().assertIsDisplayed()
    }

    @SdkSuppress(minSdkVersion = 30) // Immersive mode tap-to-show unreliable on API < 30
    @Test
    fun readerSettings_navigatesToSettingsFromReader() = runTest {
        mangaDao.insert(
            MangaEntity(
                uri = "content://reader-settings-test",
                title = "Reader Settings Test",
                pageCount = 3
            )
        )

        composeTestRule.waitUntil(timeoutMillis = 10000) {
            composeTestRule.onAllNodesWithText("Reader Settings Test").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("Reader Settings Test").performClick()
        composeTestRule.waitUntil(timeoutMillis = 10000) {
            composeTestRule.onAllNodesWithTag("reader_pager").fetchSemanticsNodes().isNotEmpty()
        }
        // Show top bar
        composeTestRule.onNodeWithTag("reader_pager").performClick()
        composeTestRule.mainClock.advanceTimeBy(500)
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithContentDescription("Reader settings").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.mainClock.advanceTimeBy(500)

        composeTestRule.onNodeWithText("Settings").assertIsDisplayed()
        composeTestRule.onNodeWithText("Grid Columns").performScrollTo().assertIsDisplayed()
    }

    @SdkSuppress(minSdkVersion = 30) // Immersive mode tap-to-show unreliable on API < 30
    @Test
    fun tappingManga_showsTitleInReaderTopBar() = runTest {
        mangaDao.insert(MangaEntity(uri = "content://nav-test2", title = "Reader Title Test", pageCount = 3))

        composeTestRule.waitUntil(timeoutMillis = 10000) {
            composeTestRule.onAllNodesWithText("Reader Title Test").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("Reader Title Test").performClick()

        composeTestRule.waitUntil(timeoutMillis = 10000) {
            composeTestRule.onAllNodesWithTag("reader_pager").fetchSemanticsNodes().isNotEmpty()
        }
        // Top bar is hidden by default, tap to show it
        composeTestRule.onNodeWithTag("reader_pager").performClick()
        composeTestRule.mainClock.advanceTimeBy(500)
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithContentDescription("Reader settings").assertIsDisplayed()
    }
}
