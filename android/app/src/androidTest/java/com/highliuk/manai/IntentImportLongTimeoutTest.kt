package com.highliuk.manai

import android.content.Intent
import android.net.Uri
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.highliuk.manai.data.local.dao.MangaDao
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import javax.inject.Inject

/**
 * Same as IntentImportTest but with 60s timeout.
 * If this also fails, the issue is a lost SharedFlow emission (race condition),
 * not a slow device timing out.
 */
@HiltAndroidTest
class IntentImportLongTimeoutTest {

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
    fun launchWithActionView_importsMangaAndNavigatesToReader_60sTimeout() =
        runTest {
            val intent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("file:///test/intent-import-long.pdf"),
                ApplicationProvider.getApplicationContext(),
                MainActivity::class.java
            )

            ActivityScenario.launch<MainActivity>(intent).use {
                composeTestRule.waitUntil(timeoutMillis = 60000) {
                    composeTestRule.onAllNodesWithTag("reader_pager")
                        .fetchSemanticsNodes().isNotEmpty()
                }

                val allManga = mangaDao.getAll().first()
                assertEquals(1, allManga.size)
                assertEquals("intent-import-long", allManga[0].title)

                composeTestRule
                    .onNodeWithTag("reader_pager")
                    .assertIsDisplayed()
            }
        }
}
