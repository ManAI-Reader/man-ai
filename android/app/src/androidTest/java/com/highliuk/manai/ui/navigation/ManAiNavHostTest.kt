package com.highliuk.manai.ui.navigation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import com.highliuk.manai.MainActivity
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

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    @Test
    fun tappingManga_navigatesToReaderScreen() = runTest {
        mangaDao.insert(MangaEntity(uri = "content://nav-test", title = "Nav Test Manga", pageCount = 5))

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Nav Test Manga").assertIsDisplayed()
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

        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithTag("reader_pager").fetchSemanticsNodes().isNotEmpty()
        }

        composeTestRule.onNodeWithTag("reader_pager").assertIsDisplayed()
    }

    @Test
    fun navigatingToReader_hidesStatusBar() = runTest {
        mangaDao.insert(MangaEntity(uri = "content://immersive-test", title = "Immersive Test", pageCount = 3))

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Immersive Test").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.mainClock.advanceTimeBy(500)
        composeTestRule.waitForIdle()

        val window = composeTestRule.activity.window
        val view = window.decorView
        val insets = view.rootWindowInsets
        val statusBarsVisible = insets?.isVisible(WindowInsetsCompat.Type.statusBars()) ?: true

        assertFalse("Status bar should be hidden in reader immersive mode", statusBarsVisible)
    }

    @Test
    fun navigatingBackFromReader_restoresStatusBar() = runTest {
        mangaDao.insert(MangaEntity(uri = "content://restore-test", title = "Restore Test", pageCount = 3))

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Restore Test").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.mainClock.advanceTimeBy(500)
        composeTestRule.waitForIdle()

        // First verify status bar IS hidden in reader
        val window = composeTestRule.activity.window
        val view = window.decorView
        val insetsInReader = view.rootWindowInsets
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

        val insetsAfterBack = view.rootWindowInsets
        val statusBarsVisible = insetsAfterBack?.isVisible(WindowInsetsCompat.Type.statusBars()) ?: false

        assertTrue("Status bar should be restored after leaving reader", statusBarsVisible)
    }

    @Test
    fun tappingManga_showsTitleInReaderTopBar() = runTest {
        mangaDao.insert(MangaEntity(uri = "content://nav-test2", title = "Reader Title Test", pageCount = 3))

        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithText("Reader Title Test").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("Reader Title Test").performClick()

        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithTag("reader_pager").fetchSemanticsNodes().isNotEmpty()
        }
        // Top bar is hidden by default, tap to show it
        composeTestRule.onNodeWithTag("reader_pager").performClick()
        composeTestRule.mainClock.advanceTimeBy(500)
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithContentDescription("Reader settings").assertIsDisplayed()
    }
}
