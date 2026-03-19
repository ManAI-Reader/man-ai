package com.highliuk.manai.ui.reader

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset

class ReaderGestureState {
    var areBarsVisible by mutableStateOf(false)
        private set

    var scale by mutableFloatStateOf(1f)
        private set

    var offsetX by mutableFloatStateOf(0f)
        private set

    var offsetY by mutableFloatStateOf(0f)
        private set

    internal var contentWidth by mutableFloatStateOf(0f)
        private set
    internal var contentHeight by mutableFloatStateOf(0f)
        private set

    val isZoomed: Boolean
        get() = scale > 1f

    fun setContentSize(width: Float, height: Float) {
        contentWidth = width
        contentHeight = height
    }

    fun toggleBars() {
        areBarsVisible = !areBarsVisible
    }

    fun onZoom(
        zoomChange: Float,
        centroid: Offset,
        containerWidth: Float,
        containerHeight: Float,
    ) {
        val oldScale = scale
        val newScale = (scale * zoomChange).coerceIn(MIN_SCALE, MAX_SCALE)
        scale = newScale

        if (!isZoomed) {
            offsetX = 0f
            offsetY = 0f
            return
        }

        val centroidRelX = centroid.x - containerWidth / 2f
        val centroidRelY = centroid.y - containerHeight / 2f
        offsetX = centroidRelX - (centroidRelX - offsetX) * (newScale / oldScale)
        offsetY = centroidRelY - (centroidRelY - offsetY) * (newScale / oldScale)

        val (maxX, maxY) = computeMaxOffsets(newScale, containerWidth, containerHeight)
        offsetX = offsetX.coerceIn(-maxX, maxX)
        offsetY = offsetY.coerceIn(-maxY, maxY)
    }

    fun resetZoom() {
        scale = MIN_SCALE
        offsetX = 0f
        offsetY = 0f
    }

    internal fun computeMaxOffsets(
        scale: Float,
        containerWidth: Float,
        containerHeight: Float,
    ): Pair<Float, Float> {
        val hasValidDimensions = contentWidth > 0f && contentHeight > 0f &&
            containerHeight > 0f && containerWidth > 0f
        if (hasValidDimensions) {
            val isPortraitContent = contentWidth <= contentHeight
            val isPortraitContainer = containerWidth <= containerHeight
            return if (isPortraitContent && isPortraitContainer) {
                val renderedHeight = containerWidth * contentHeight / contentWidth
                val maxX = containerWidth * (scale - 1f) / 2f
                val maxY = (scale * renderedHeight / 2f - containerHeight / 2f).coerceAtLeast(0f)
                maxX to maxY
            } else {
                val renderedWidth = containerHeight * contentWidth / contentHeight
                val maxX = (scale * renderedWidth / 2f - containerWidth / 2f).coerceAtLeast(0f)
                val maxY = containerHeight * (scale - 1f) / 2f
                maxX to maxY
            }
        }
        return Pair(
            containerWidth * (scale - 1f) / 2f,
            containerHeight * (scale - 1f) / 2f
        )
    }

    fun onPan(panX: Float, panY: Float, containerWidth: Float, containerHeight: Float) {
        if (!isZoomed) return
        val (maxOffsetX, maxOffsetY) = computeMaxOffsets(scale, containerWidth, containerHeight)
        offsetX = (offsetX + panX).coerceIn(
            minOf(-maxOffsetX, offsetX),
            maxOf(maxOffsetX, offsetX),
        )
        offsetY = (offsetY + panY).coerceIn(
            minOf(-maxOffsetY, offsetY),
            maxOf(maxOffsetY, offsetY),
        )
    }

    fun onPanX(panX: Float, containerWidth: Float) {
        if (!isZoomed) return
        val maxOffsetX = containerWidth * (scale - 1f) / 2f
        offsetX = (offsetX + panX).coerceIn(
            minOf(-maxOffsetX, offsetX),
            maxOf(maxOffsetX, offsetX),
        )
    }

    fun onDoubleTapWebtoon(
        tapX: Float,
        tapY: Float = 0f,
        containerWidth: Float,
        containerHeight: Float = 0f,
    ): ZoomTarget {
        return if (!isZoomed) {
            val targetScale = DOUBLE_TAP_SCALE
            val maxOffsetX = containerWidth * (targetScale - 1f) / 2f
            val centerX = containerWidth / 2f
            val targetOffsetX = (centerX - tapX).coerceIn(-maxOffsetX, maxOffsetX)
            val maxOffsetY = containerHeight * (targetScale - 1f) / 2f
            val centerY = containerHeight / 2f
            val targetOffsetY = (centerY - tapY).coerceIn(-maxOffsetY, maxOffsetY)
            ZoomTarget(targetScale, targetOffsetX, targetOffsetY)
        } else {
            ZoomTarget(MIN_SCALE, 0f, 0f)
        }
    }

    fun applyZoomTarget(target: ZoomTarget) {
        scale = target.scale
        offsetX = target.offsetX
        offsetY = target.offsetY
    }

    fun onDoubleTap(tapX: Float, tapY: Float, containerWidth: Float, containerHeight: Float): ZoomTarget {
        return if (!isZoomed) {
            val isLandscape = containerWidth > containerHeight
            val targetScale = if (isLandscape) DOUBLE_TAP_SCALE_LANDSCAPE else DOUBLE_TAP_SCALE
            val maxOffsetX = containerWidth * (targetScale - 1f) / 2f
            val maxOffsetY = containerHeight * (targetScale - 1f) / 2f
            val centerX = containerWidth / 2f
            val centerY = containerHeight / 2f
            val targetOffsetX = ((centerX - tapX) * (targetScale - 1f)).coerceIn(-maxOffsetX, maxOffsetX)
            val targetOffsetY = ((centerY - tapY) * (targetScale - 1f)).coerceIn(-maxOffsetY, maxOffsetY)
            ZoomTarget(targetScale, targetOffsetX, targetOffsetY)
        } else {
            ZoomTarget(MIN_SCALE, 0f, 0f)
        }
    }

    companion object {
        const val MIN_SCALE = 1f
        const val MAX_SCALE = 5f
        const val DOUBLE_TAP_SCALE = 2f
        const val DOUBLE_TAP_SCALE_LANDSCAPE = 3f
    }
}

data class ZoomTarget(val scale: Float, val offsetX: Float, val offsetY: Float)
