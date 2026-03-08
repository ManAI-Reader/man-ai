package com.highliuk.manai.ui.reader

import androidx.compose.ui.layout.ContentScale

internal fun chooseContentScale(
    imageWidth: Float,
    imageHeight: Float,
    containerWidth: Float,
    containerHeight: Float,
): ContentScale {
    val hasValidDimensions = containerHeight > 0f && containerWidth > 0f &&
        imageWidth > 0f && imageHeight > 0f
    if (!hasValidDimensions) {
        return ContentScale.FillWidth
    }
    val imageAspectRatio = imageWidth / imageHeight
    val containerAspectRatio = containerWidth / containerHeight
    return if (imageAspectRatio <= 1f && containerAspectRatio <= 1f) {
        ContentScale.FillWidth
    } else {
        ContentScale.FillHeight
    }
}
