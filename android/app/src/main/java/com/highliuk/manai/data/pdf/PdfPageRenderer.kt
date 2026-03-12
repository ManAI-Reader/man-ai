package com.highliuk.manai.data.pdf

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PdfPageRenderer @Inject constructor(
    private val contentResolver: ContentResolver,
) {
    suspend fun render(uri: String, pageIndex: Int): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val pfd = contentResolver.openFileDescriptor(Uri.parse(uri), "r")
                ?: return@withContext null
            pfd.use { fd ->
                PdfRenderer(fd).use { renderer ->
                    if (pageIndex >= renderer.pageCount) return@withContext null
                    renderer.openPage(pageIndex).use { page ->
                        val bmp = Bitmap.createBitmap(
                            page.width, page.height, Bitmap.Config.ARGB_8888,
                        )
                        page.render(
                            bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY,
                        )
                        bmp
                    }
                }
            }
        } catch (@Suppress("TooGenericExceptionCaught") _: Exception) {
            null
        }
    }
}
