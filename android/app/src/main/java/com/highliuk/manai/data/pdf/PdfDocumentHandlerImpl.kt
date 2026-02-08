package com.highliuk.manai.data.pdf

import android.content.ContentResolver
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.provider.OpenableColumns
import com.highliuk.manai.domain.model.PdfMetadata
import com.highliuk.manai.domain.repository.PdfDocumentHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class PdfDocumentHandlerImpl @Inject constructor(
    private val contentResolver: ContentResolver,
) : PdfDocumentHandler {

    override suspend fun importDocument(uriString: String): PdfMetadata =
        withContext(Dispatchers.IO) {
            val uri = Uri.parse(uriString)
            takePersistablePermission(uri)
            val title = getDisplayName(uri)
            val pageCount = getPageCount(uri)
            PdfMetadata(title = title, pageCount = pageCount)
        }

    /**
     * Renders a single page of the PDF as a Bitmap.
     * Used by the UI to generate cover thumbnails.
     */
    suspend fun renderPage(uriString: String, pageIndex: Int, width: Int): Bitmap? =
        withContext(Dispatchers.IO) {
            val uri = Uri.parse(uriString)
            val fd = contentResolver.openFileDescriptor(uri, "r") ?: return@withContext null
            fd.use { descriptor ->
                val renderer = PdfRenderer(descriptor)
                renderer.use { pdf ->
                    if (pageIndex >= pdf.pageCount) return@withContext null
                    val page = pdf.openPage(pageIndex)
                    page.use {
                        val scale = width.toFloat() / it.width
                        val height = (it.height * scale).toInt()
                        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                        it.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        bitmap
                    }
                }
            }
        }

    private fun takePersistablePermission(uri: Uri) {
        try {
            contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
        } catch (_: SecurityException) {
            // Permission may already be persisted or not available
        }
    }

    private fun getDisplayName(uri: Uri): String {
        contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val name = cursor.getString(0)
                    // Strip .pdf extension for a cleaner title
                    return name.removeSuffix(".pdf").removeSuffix(".PDF")
                }
            }
        // Fallback: use last path segment
        return uri.lastPathSegment?.removeSuffix(".pdf") ?: "Unknown"
    }

    private fun getPageCount(uri: Uri): Int {
        val fd = contentResolver.openFileDescriptor(uri, "r")
            ?: throw IllegalStateException("Cannot open PDF file descriptor")
        return fd.use { descriptor ->
            val renderer = PdfRenderer(descriptor)
            renderer.use { it.pageCount }
        }
    }
}
