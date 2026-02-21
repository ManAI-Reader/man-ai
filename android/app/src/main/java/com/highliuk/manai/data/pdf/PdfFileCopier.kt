package com.highliuk.manai.data.pdf

interface PdfFileCopier {
    suspend fun copyToLocalStorage(sourceUri: String): String
}
