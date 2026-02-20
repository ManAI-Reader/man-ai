package com.highliuk.manai.data.hash

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.security.MessageDigest
import javax.inject.Inject

class FileHashProviderImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : FileHashProvider {

    override fun computeHash(inputStream: InputStream): String {
        val digest = MessageDigest.getInstance("MD5")
        val buffer = ByteArray(BUFFER_SIZE)
        var bytesRead = inputStream.read(buffer)
        while (bytesRead != -1) {
            digest.update(buffer, 0, bytesRead)
            bytesRead = inputStream.read(buffer)
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    override suspend fun computeHash(uri: String): String = withContext(Dispatchers.IO) {
        val inputStream = context.contentResolver.openInputStream(Uri.parse(uri))
            ?: throw IllegalArgumentException("Cannot open file: $uri")
        inputStream.use { computeHash(it) }
    }

    companion object {
        private const val BUFFER_SIZE = 8192
    }
}
