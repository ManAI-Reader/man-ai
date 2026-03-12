package com.highliuk.manai.domain.ml

import android.graphics.Bitmap

interface TextRecognizer {
    suspend fun initialize()
    suspend fun recognize(bitmap: Bitmap, region: TextRegion): OcrResult
}
