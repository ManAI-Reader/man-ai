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
        if (bitmapWidth <= 0 || bitmapHeight <= 0) {
            return OverlayRect(0f, 0f, 0f, 0f)
        }
        val imageAspect = bitmapWidth.toFloat() / bitmapHeight
        val containerAspect = containerWidth / containerHeight
        val useFillWidth = imageAspect <= 1f && containerAspect <= 1f

        return if (useFillWidth) {
            val scale = containerWidth / bitmapWidth
            val offsetY = (containerHeight - bitmapHeight * scale) / 2f
            OverlayRect(
                left = region.normX1 * bitmapWidth * scale,
                top = region.normY1 * bitmapHeight * scale + offsetY,
                right = region.normX2 * bitmapWidth * scale,
                bottom = region.normY2 * bitmapHeight * scale + offsetY,
            )
        } else {
            val scale = containerHeight / bitmapHeight
            val offsetX = (containerWidth - bitmapWidth * scale) / 2f
            OverlayRect(
                left = region.normX1 * bitmapWidth * scale + offsetX,
                top = region.normY1 * bitmapHeight * scale,
                right = region.normX2 * bitmapWidth * scale + offsetX,
                bottom = region.normY2 * bitmapHeight * scale,
            )
        }
    }
}
