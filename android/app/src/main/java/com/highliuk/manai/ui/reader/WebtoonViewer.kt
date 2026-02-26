package com.highliuk.manai.ui.reader

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.platform.testTag
import kotlinx.coroutines.launch

private const val DOUBLE_TAP_ANIM_DURATION = 300

@Suppress("LongMethod")
@Composable
fun WebtoonViewer(
    lazyListState: LazyListState,
    uri: String,
    pageCount: Int,
    gestureState: ReaderGestureState,
    modifier: Modifier = Modifier,
) {
    val coroutineScope = rememberCoroutineScope()

    LazyColumn(
        state = lazyListState,
        userScrollEnabled = true,
        modifier = modifier
            .fillMaxSize()
            .testTag("webtoon_viewer")
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { gestureState.toggleBars() },
                    onDoubleTap = { offset ->
                        val target = gestureState.onDoubleTapWebtoon(
                            tapX = offset.x,
                            containerWidth = size.width.toFloat()
                        )
                        coroutineScope.launch {
                            val startScale = gestureState.scale
                            val startOffsetX = gestureState.offsetX
                            val anim = Animatable(0f)
                            anim.animateTo(1f, tween(DOUBLE_TAP_ANIM_DURATION)) {
                                val progress = value
                                gestureState.applyZoomTarget(
                                    ZoomTarget(
                                        scale = startScale + (target.scale - startScale) * progress,
                                        offsetX = startOffsetX + (target.offsetX - startOffsetX) * progress,
                                        offsetY = 0f
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

                        if (gestureState.isZoomed && panChange.x != 0f) {
                            gestureState.onPanX(panChange.x, size.width.toFloat())
                        }
                    } while (event.changes.any { it.pressed })
                }
            }
            .graphicsLayer {
                scaleX = gestureState.scale
                scaleY = gestureState.scale
                translationX = gestureState.offsetX
            },
    ) {
        items(pageCount) { pageIndex ->
            PdfPage(
                uri = uri,
                pageIndex = pageIndex,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
