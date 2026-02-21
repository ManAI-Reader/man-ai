package com.highliuk.manai.data.pdf

interface PdfFileManager {
    suspend fun copyToLocalStorage(sourceUri: String): String
    suspend fun deleteLocalCopy(uri: String): Boolean
}
