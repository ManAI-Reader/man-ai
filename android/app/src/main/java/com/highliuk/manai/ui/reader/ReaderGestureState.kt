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

    private var contentWidth = 0f
    private var contentHeight = 0f

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

    fun onPan(panX: Float, panY: Float, containerWidth: Float, containerHeight: Float) {
        if (!isZoomed) return
        val maxOffsetX = containerWidth * (scale - 1f) / 2f

        val maxOffsetY = if (contentWidth > 0f && contentHeight > 0f) {
            val renderedImageHeight = containerWidth * contentHeight / contentWidth
            (scale * renderedImageHeight / 2f - containerHeight / 2f).coerceAtLeast(0f)
        } else {
            containerHeight * (scale - 1f) / 2f
        }

        offsetX = (offsetX + panX).coerceIn(-maxOffsetX, maxOffsetX)
        offsetY = (offsetY + panY).coerceIn(-maxOffsetY, maxOffsetY)
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
        const val MAX_SCALE = 3f
        const val DOUBLE_TAP_SCALE = 2f
    }
}

data class ZoomTarget(val scale: Float, val offsetX: Float, val offsetY: Float)
