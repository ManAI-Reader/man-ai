package com.highliuk.manai.data.pdf

interface PdfMetadataExtractor {
    suspend fun extractPageCount(uri: String): Int
}
