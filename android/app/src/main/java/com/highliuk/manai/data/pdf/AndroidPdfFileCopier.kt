package com.highliuk.manai.data.pdf

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import javax.inject.Inject

class AndroidPdfFileCopier @Inject constructor(
    @ApplicationContext private val context: Context
) : PdfFileCopier {

    override suspend fun copyToLocalStorage(sourceUri: String): String =
        withContext(Dispatchers.IO) {
            val inputStream = context.contentResolver.openInputStream(Uri.parse(sourceUri))
                ?: throw IllegalArgumentException("Cannot open URI: $sourceUri")

            val mangaDir = context.getExternalFilesDir("manga")
            val localFile = File(mangaDir, "${UUID.randomUUID()}.pdf")

            inputStream.use { input ->
                localFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            "file://${localFile.absolutePath}"
        }
}
