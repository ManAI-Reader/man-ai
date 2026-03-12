package com.highliuk.manai.domain.ml

import android.graphics.Bitmap

interface TextDetector {
    suspend fun initialize()
    suspend fun detect(bitmap: Bitmap): List<TextRegion>
}
