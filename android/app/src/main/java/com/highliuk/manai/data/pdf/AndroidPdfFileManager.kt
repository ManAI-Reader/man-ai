package com.highliuk.manai.data.pdf

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import javax.inject.Inject

class AndroidPdfFileManager @Inject constructor(
    @ApplicationContext private val context: Context
) : PdfFileManager {

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

    override suspend fun deleteLocalCopy(uri: String): Boolean =
        withContext(Dispatchers.IO) {
            if (!uri.startsWith("file://")) return@withContext false
            val path = uri.removePrefix("file://")
            val file = File(path)
            val mangaDir = context.getExternalFilesDir("manga") ?: return@withContext false
            if (!file.absolutePath.startsWith(mangaDir.absolutePath)) return@withContext false
            file.delete()
        }
}
