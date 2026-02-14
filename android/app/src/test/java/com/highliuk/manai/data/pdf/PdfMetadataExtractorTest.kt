package com.highliuk.manai.data.pdf

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class PdfMetadataExtractorTest {

    private val extractor = mockk<PdfMetadataExtractor>()

    @Test
    fun `extractPageCount returns page count for given uri`() = runTest {
        coEvery { extractor.extractPageCount("content://test.pdf") } returns 42

        val result = extractor.extractPageCount("content://test.pdf")
        assertEquals(42, result)
    }
}
