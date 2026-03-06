package com.highliuk.manai.ui.reader

private const val LEFT_BOUNDARY = 1f / 3f
private const val RIGHT_BOUNDARY = 2f / 3f

enum class TapZone { LEFT, CENTER, RIGHT }

fun classifyTapZone(tapX: Float, containerWidth: Float): TapZone {
    val ratio = tapX / containerWidth
    return when {
        ratio <= LEFT_BOUNDARY -> TapZone.LEFT
        ratio >= RIGHT_BOUNDARY -> TapZone.RIGHT
        else -> TapZone.CENTER
    }
}
