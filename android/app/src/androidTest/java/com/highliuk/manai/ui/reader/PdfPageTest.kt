package com.highliuk.manai.ui.reader

import android.net.Uri
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.File

class PdfPageTest {

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
    fun rendersPageImage_whenValidPdfProvided() {
        var loadedWidth = 0
        var loadedHeight = 0

        composeTestRule.setContent {
            PdfPage(
                uri = testPdfUri,
                pageIndex = 0,
                onBitmapLoaded = { w, h ->
                    loadedWidth = w
                    loadedHeight = h
                }
            )
        }

        composeTestRule.waitUntil(timeoutMillis = 5_000) { loadedWidth > 0 }
        assertTrue("Bitmap width should be > 0", loadedWidth > 0)
        assertTrue("Bitmap height should be > 0", loadedHeight > 0)
    }

    @Test
    fun rendersPageImage_whenContentScaleIsFillHeight() {
        var bitmapLoaded = false

        composeTestRule.setContent {
            PdfPage(
                uri = testPdfUri,
                pageIndex = 0,
                contentScale = ContentScale.FillHeight,
                onBitmapLoaded = { _, _ -> bitmapLoaded = true }
            )
        }

        composeTestRule.waitUntil(timeoutMillis = 5_000) { bitmapLoaded }
    }

    @Test
    fun showsPlaceholder_whenInvalidUriProvided() {
        composeTestRule.setContent {
            PdfPage(
                uri = "file:///nonexistent.pdf",
                pageIndex = 0
            )
        }

        composeTestRule.waitForIdle()
        composeTestRule.mainClock.advanceTimeBy(1000)
        composeTestRule.onNodeWithContentDescription("PDF placeholder").assertIsDisplayed()
    }

    @Test
    fun showsPlaceholder_whenPageIndexOutOfRange() {
        composeTestRule.setContent {
            PdfPage(
                uri = testPdfUri,
                pageIndex = 999
            )
        }

        composeTestRule.waitForIdle()
        composeTestRule.mainClock.advanceTimeBy(1000)
        composeTestRule.onNodeWithContentDescription("PDF placeholder").assertIsDisplayed()
    }
}
