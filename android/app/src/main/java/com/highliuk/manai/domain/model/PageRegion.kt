package com.highliuk.manai.domain.model

data class PageRegion(
    val regionIndex: Int,
    val normX1: Float,
    val normY1: Float,
    val normX2: Float,
    val normY2: Float,
    val confidence: Float,
    val ocrText: String?,
    val pageIndex: Int = 0,
)
