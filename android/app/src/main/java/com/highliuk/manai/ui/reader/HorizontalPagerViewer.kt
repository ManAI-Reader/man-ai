package com.highliuk.manai.ui.reader

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.platform.testTag
import kotlinx.coroutines.launch

private const val DOUBLE_TAP_ANIM_DURATION = 300

@Suppress("LongMethod")
@Composable
fun HorizontalPagerViewer(
    pagerState: PagerState,
    uri: String,
    isRtl: Boolean,
    gestureState: ReaderGestureState,
    modifier: Modifier = Modifier,
) {
    val coroutineScope = rememberCoroutineScope()

    HorizontalPager(
        state = pagerState,
        reverseLayout = isRtl,
        userScrollEnabled = !gestureState.isZoomed,
        modifier = modifier
            .fillMaxSize()
            .testTag("reader_pager")
    ) { pageIndex ->
        PdfPage(
            uri = uri,
            pageIndex = pageIndex,
            onBitmapLoaded = { w, h ->
                gestureState.setContentSize(w.toFloat(), h.toFloat())
            },
            modifier = Modifier
                .fillMaxSize()
                .testTag("reader_zoom_container")
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { gestureState.toggleBars() },
                        onDoubleTap = { offset ->
                            val target = gestureState.onDoubleTap(
                                tapX = offset.x,
                                tapY = offset.y,
                                containerWidth = size.width.toFloat(),
                                containerHeight = size.height.toFloat()
                            )
                            coroutineScope.launch {
                                val startScale = gestureState.scale
                                val startOffsetX = gestureState.offsetX
                                val startOffsetY = gestureState.offsetY
                                val anim = Animatable(0f)
                                anim.animateTo(1f, tween(DOUBLE_TAP_ANIM_DURATION)) {
                                    val progress = value
                                    gestureState.applyZoomTarget(
                                        ZoomTarget(
                                            scale = startScale + (target.scale - startScale) * progress,
                                            offsetX = startOffsetX + (target.offsetX - startOffsetX) * progress,
                                            offsetY = startOffsetY + (target.offsetY - startOffsetY) * progress
                                        )
                                    )
                                }
                            }
                        }
                    )
                }
                .pointerInput(Unit) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false)
                        do {
                            val event = awaitPointerEvent()
                            val zoomChange = event.calculateZoom()
                            val panChange = event.calculatePan()

                            if (zoomChange != 1f) {
                                gestureState.onZoom(zoomChange)
                                event.changes.forEach { if (it.positionChanged()) it.consume() }
                            }

                            if (gestureState.isZoomed && panChange != Offset.Zero) {
                                gestureState.onPan(
                                    panChange.x, panChange.y,
                                    size.width.toFloat(), size.height.toFloat()
                                )
                                event.changes.forEach { if (it.positionChanged()) it.consume() }
                            }
                        } while (event.changes.any { it.pressed })
                    }
                }
                .graphicsLayer {
                    scaleX = gestureState.scale
                    scaleY = gestureState.scale
                    translationX = gestureState.offsetX
                    translationY = gestureState.offsetY
                }
        )
    }
}
