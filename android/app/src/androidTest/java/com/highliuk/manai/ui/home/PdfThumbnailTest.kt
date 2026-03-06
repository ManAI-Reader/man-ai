package com.highliuk.manai.ui.home

import android.net.Uri
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.File

class PdfThumbnailTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var testPdfUri: String

    @Before
    fun setUp() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val testContext = InstrumentationRegistry.getInstrumentation().context
        val testDir = File(appContext.filesDir, "test-pdfs").apply { mkdirs() }
        val testPdf = File(testDir, "test.pdf")
        testContext.assets.open("test.pdf").use { input ->
            testPdf.outputStream().use { output -> input.copyTo(output) }
        }
        testPdfUri = Uri.fromFile(testPdf).toString()
    }

    @Test
    fun rendersThumbnail_whenValidPdfProvided() {
        composeTestRule.setContent {
            PdfThumbnail(uri = testPdfUri, mangaId = 42L)
        }

        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodes(
                androidx.compose.ui.test.hasContentDescription("PDF thumbnail")
            ).fetchSemanticsNodes().isNotEmpty()
        }

        composeTestRule.onNodeWithContentDescription("PDF thumbnail").assertIsDisplayed()
    }

    @Test
    fun cachesRenderedThumbnail_whenMangaIdProvided() {
        val mangaId = System.nanoTime()
        assertNull(thumbnailCache.get(mangaId))

        composeTestRule.setContent {
            PdfThumbnail(uri = testPdfUri, mangaId = mangaId)
        }

        composeTestRule.waitUntil(timeoutMillis = 5000) {
            thumbnailCache.get(mangaId) != null
        }

        assertNotNull(thumbnailCache.get(mangaId))
    }

    @Test
    fun showsPlaceholder_whenInvalidUri() {
        composeTestRule.setContent {
            PdfThumbnail(uri = "file:///nonexistent.pdf")
        }

        composeTestRule.waitForIdle()
        composeTestRule.mainClock.advanceTimeBy(1000)
        composeTestRule.onNodeWithContentDescription("PDF placeholder").assertIsDisplayed()
    }
}
