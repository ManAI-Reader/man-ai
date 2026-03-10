package com.highliuk.manai.ui.reader

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import com.highliuk.manai.domain.model.BalloonPipelineStatus
import com.highliuk.manai.domain.model.PagePipelineState
import com.highliuk.manai.domain.model.PageRegion

@Composable
fun DebugMlOverlay(
    pageState: PagePipelineState?,
    regions: List<PageRegion>,
    bitmapWidth: Int,
    bitmapHeight: Int,
    modifier: Modifier = Modifier,
) {
    if (pageState == null) return

    Canvas(modifier = modifier.fillMaxSize().testTag("debug_ml_overlay")) {
        drawRect(
            color = Color(pageState.pageStatus.overlayColor),
            size = size,
        )

        if (bitmapWidth <= 0 || bitmapHeight <= 0) return@Canvas

        for (region in regions) {
            val rect = OverlayCoordinateMapper.mapRegion(
                region, bitmapWidth, bitmapHeight, size.width, size.height,
            )
            val rectSize = Size(rect.right - rect.left, rect.bottom - rect.top)

            drawRect(
                color = Color.White,
                topLeft = Offset(rect.left, rect.top),
                size = rectSize,
                style = Stroke(width = 2f),
            )

            val balloonStatus = pageState.balloonStatuses[region.regionIndex] ?: continue
            drawRect(
                color = Color(balloonStatus.overlayColor),
                topLeft = Offset(rect.left, rect.top),
                size = rectSize,
            )

            if (balloonStatus is BalloonPipelineStatus.OcrQueued) {
                drawContext.canvas.nativeCanvas.drawText(
                    balloonStatus.position.toString(),
                    rect.left + rectSize.width / 2,
                    rect.top + rectSize.height / 2,
                    android.graphics.Paint().apply {
                        color = android.graphics.Color.WHITE
                        textSize = QUEUE_NUMBER_TEXT_SIZE
                        textAlign = android.graphics.Paint.Align.CENTER
                        isFakeBoldText = true
                        setShadowLayer(SHADOW_RADIUS, SHADOW_DX, SHADOW_DY, android.graphics.Color.BLACK)
                    }
                )
            }
        }
    }
}

private const val QUEUE_NUMBER_TEXT_SIZE = 48f
private const val SHADOW_RADIUS = 4f
private const val SHADOW_DX = 2f
private const val SHADOW_DY = 2f
