package com.highliuk.manai.data.pdf

import android.content.Context
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class AndroidPdfMetadataExtractor @Inject constructor(
    @ApplicationContext private val context: Context
) : PdfMetadataExtractor {

    override suspend fun extractPageCount(uri: String): Int {
        val pfd = context.contentResolver.openFileDescriptor(Uri.parse(uri), "r")
            ?: throw IllegalArgumentException("Cannot open PDF: $uri")
        return pfd.use { fd ->
            PdfRenderer(fd).use { renderer ->
                renderer.pageCount
            }
        }
    }
}
