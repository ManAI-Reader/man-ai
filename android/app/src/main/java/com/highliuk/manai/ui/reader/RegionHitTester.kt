package com.highliuk.manai.ui.reader

import com.highliuk.manai.domain.model.PageRegion

object RegionHitTester {

    @Suppress("LongParameterList")
    internal fun screenToNormalized(
        tapX: Float, tapY: Float,
        containerWidth: Float, containerHeight: Float,
        bitmapWidth: Int, bitmapHeight: Int,
        scale: Float, offsetX: Float, offsetY: Float,
    ): Pair<Float, Float>? {
        val centerX = containerWidth / 2f
        val centerY = containerHeight / 2f

        // Invert graphicsLayer transform
        val contentX = (tapX - centerX) / scale + centerX - offsetX / scale
        val contentY = (tapY - centerY) / scale + centerY - offsetY / scale

        val imageAspect = bitmapWidth.toFloat() / bitmapHeight
        val containerAspect = containerWidth / containerHeight
        val useFillWidth = imageAspect <= 1f && containerAspect <= 1f

        val normX: Float
        val normY: Float
        if (useFillWidth) {
            val renderedImageHeight = containerWidth * bitmapHeight / bitmapWidth
            val verticalPadding = (containerHeight - renderedImageHeight) / 2f
            normX = contentX / containerWidth
            normY = (contentY - verticalPadding) / renderedImageHeight
        } else {
            val renderedImageWidth = containerHeight * bitmapWidth / bitmapHeight
            val horizontalPadding = (containerWidth - renderedImageWidth) / 2f
            normX = (contentX - horizontalPadding) / renderedImageWidth
            normY = contentY / containerHeight
        }

        val inBounds = normX in 0f..1f && normY in 0f..1f
        if (!inBounds) return null
        return normX to normY
    }

    @Suppress("LongParameterList")
    fun hitTest(
        tapX: Float, tapY: Float,
        regions: List<PageRegion>,
        containerWidth: Float, containerHeight: Float,
        bitmapWidth: Int, bitmapHeight: Int,
        scale: Float, offsetX: Float, offsetY: Float,
    ): PageRegion? {
        val (normX, normY) = screenToNormalized(
            tapX, tapY, containerWidth, containerHeight,
            bitmapWidth, bitmapHeight, scale, offsetX, offsetY,
        ) ?: return null

        return regions
            .filter { normX in it.normX1..it.normX2 && normY in it.normY1..it.normY2 }
            .maxByOrNull { it.confidence }
    }
}
