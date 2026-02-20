package com.highliuk.manai.data.hash

import java.io.InputStream

interface FileHashProvider {
    fun computeHash(inputStream: InputStream): String
    suspend fun computeHash(uri: String): String
}
