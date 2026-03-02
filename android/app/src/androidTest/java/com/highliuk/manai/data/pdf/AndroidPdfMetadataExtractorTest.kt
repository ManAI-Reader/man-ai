package com.highliuk.manai.data.pdf

import android.net.Uri
import androidx.core.content.FileProvider
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import java.io.File

class AndroidPdfMetadataExtractorTest {

    private lateinit var extractor: AndroidPdfMetadataExtractor
    private lateinit var testPdfUri: String

    @Before
    fun setUp() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val testContext = InstrumentationRegistry.getInstrumentation().context
        extractor = AndroidPdfMetadataExtractor(appContext)

        val testDir = File(appContext.filesDir, "test-pdfs").apply { mkdirs() }
        val testPdf = File(testDir, "test.pdf")
        testContext.assets.open("test.pdf").use { input ->
            testPdf.outputStream().use { output -> input.copyTo(output) }
        }
        testPdfUri = Uri.fromFile(testPdf).toString()
    }

    @Test
    fun extractPageCount_returnsOneForSinglePagePdf() = runTest {
        val pageCount = extractor.extractPageCount(testPdfUri)

        assertEquals(1, pageCount)
    }

    @Test
    fun extractPageCount_throwsForInvalidUri() = runTest {
        assertThrows(Exception::class.java) {
            kotlinx.coroutines.test.runTest {
                extractor.extractPageCount("file:///nonexistent.pdf")
            }
        }
    }
}
