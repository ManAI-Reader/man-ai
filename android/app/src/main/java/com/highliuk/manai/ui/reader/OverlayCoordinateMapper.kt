package com.highliuk.manai.ui.reader

import com.highliuk.manai.domain.model.PageRegion

data class OverlayRect(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
)

object OverlayCoordinateMapper {

    fun mapRegion(
        region: PageRegion,
        bitmapWidth: Int,
        bitmapHeight: Int,
        containerWidth: Float,
        containerHeight: Float,
    ): OverlayRect {
        // ContentScale.FillWidth: scale to fill width, center vertically
        val scale = containerWidth / bitmapWidth
        val offsetY = (containerHeight - bitmapHeight * scale) / 2f

        return OverlayRect(
            left = region.normX1 * bitmapWidth * scale,
            top = region.normY1 * bitmapHeight * scale + offsetY,
            right = region.normX2 * bitmapWidth * scale,
            bottom = region.normY2 * bitmapHeight * scale + offsetY,
        )
    }
}
