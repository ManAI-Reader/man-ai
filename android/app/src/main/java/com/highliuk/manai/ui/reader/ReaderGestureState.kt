package com.highliuk.manai.ui.reader

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

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

    fun onZoom(zoomChange: Float) {
        scale = (scale * zoomChange).coerceIn(MIN_SCALE, MAX_SCALE)
        if (!isZoomed) {
            offsetX = 0f
            offsetY = 0f
        }
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
        offsetX = (offsetX + panX).coerceIn(-maxOffsetX, maxOffsetX)
        offsetY = (offsetY + panY).coerceIn(-maxOffsetY, maxOffsetY)
    }

    fun onPanX(panX: Float, containerWidth: Float) {
        if (!isZoomed) return
        val maxOffsetX = containerWidth * (scale - 1f) / 2f
        offsetX = (offsetX + panX).coerceIn(-maxOffsetX, maxOffsetX)
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
            val targetScale = DOUBLE_TAP_SCALE
            val maxOffsetX = containerWidth * (targetScale - 1f) / 2f
            val maxOffsetY = containerHeight * (targetScale - 1f) / 2f
            val centerX = containerWidth / 2f
            val centerY = containerHeight / 2f
            val targetOffsetX = (centerX - tapX).coerceIn(-maxOffsetX, maxOffsetX)
            val targetOffsetY = (centerY - tapY).coerceIn(-maxOffsetY, maxOffsetY)
            ZoomTarget(targetScale, targetOffsetX, targetOffsetY)
        } else {
            ZoomTarget(MIN_SCALE, 0f, 0f)
        }
    }

    companion object {
        const val MIN_SCALE = 1f
        const val MAX_SCALE = 5f
        const val DOUBLE_TAP_SCALE = 2f
    }
}

data class ZoomTarget(val scale: Float, val offsetX: Float, val offsetY: Float)
