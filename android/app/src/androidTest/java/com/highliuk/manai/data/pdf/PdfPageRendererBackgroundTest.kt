package com.highliuk.manai.data.pdf

import android.content.Context
import android.graphics.Color
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.test.platform.app.InstrumentationRegistry
import com.highliuk.manai.ui.reader.renderPdfFallback
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

/**
 * A PDF page whose content stream paints nothing has an unpainted background:
 * without an explicit white fill the renderer leaves the ARGB_8888 bitmap
 * transparent, which shows as ghost pages over dark app surfaces.
 */
class PdfPageRendererBackgroundTest {

    private lateinit var context: Context
    private lateinit var pdfFile: File
    private lateinit var uri: Uri

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        pdfFile = File(context.filesDir, "background-test.pdf")
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        // Nothing is drawn on the page canvas: the background stays unpainted.
        document.finishPage(document.startPage(pageInfo))
        pdfFile.outputStream().use { document.writeTo(it) }
        document.close()
        uri = Uri.fromFile(pdfFile)
    }

    @After
    fun tearDown() {
        pdfFile.delete()
    }

    @Test
    fun rendererFillsUnpaintedPageBackgroundWithOpaqueWhite() {
        val bitmap = runBlocking {
            PdfPageRenderer(context.contentResolver).render(uri.toString(), 0)
        }

        assertNotNull("page 0 must render", bitmap)
        assertEquals(Color.WHITE, bitmap!!.getPixel(1, 1))
    }

    @Test
    fun fallbackRendererFillsUnpaintedPageBackgroundWithOpaqueWhite() {
        val bitmap = runBlocking {
            renderPdfFallback(context.contentResolver, uri.toString(), 0)
        }

        assertNotNull("page 0 must render", bitmap)
        assertEquals(Color.WHITE, bitmap!!.getPixel(1, 1))
    }

    private companion object {
        const val PAGE_WIDTH = 200
        const val PAGE_HEIGHT = 300
    }
}
