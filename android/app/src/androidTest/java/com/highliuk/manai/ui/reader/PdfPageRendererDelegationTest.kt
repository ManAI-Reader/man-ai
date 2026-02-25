package com.highliuk.manai.ui.reader

import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import com.highliuk.manai.data.pdf.PdfPageRenderer
import io.mockk.coEvery
import io.mockk.mockk
import org.junit.Rule
import org.junit.Test

class PdfPageRendererDelegationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun pdfPageUsesRendererWhenProvided() {
        val renderer = mockk<PdfPageRenderer>()
        val fakeBitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888).apply {
            eraseColor(Color.RED)
        }
        coEvery { renderer.render("content://test", 0) } returns fakeBitmap

        composeTestRule.setContent {
            PdfPage(
                uri = "content://test",
                pageIndex = 0,
                pdfPageRenderer = renderer,
            )
        }

        composeTestRule.onNodeWithContentDescription("Page 1", substring = true)
            .assertIsDisplayed()
    }
}
