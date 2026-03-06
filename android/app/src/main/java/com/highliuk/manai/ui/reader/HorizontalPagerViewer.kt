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
import androidx.compose.ui.semantics.semantics
import kotlinx.coroutines.launch

private const val DOUBLE_TAP_ANIM_DURATION = 300

private fun Modifier.pageZoom(
    isCurrentPage: Boolean,
    gestureState: ReaderGestureState,
): Modifier = this
    .semantics { set(ZoomScaleKey, if (isCurrentPage) gestureState.scale else 1f) }
    .graphicsLayer {
        scaleX = if (isCurrentPage) gestureState.scale else 1f
        scaleY = if (isCurrentPage) gestureState.scale else 1f
        translationX = if (isCurrentPage) gestureState.offsetX else 0f
        translationY = if (isCurrentPage) gestureState.offsetY else 0f
    }

@Suppress("LongMethod", "LongParameterList")
@Composable
fun HorizontalPagerViewer(
    pagerState: PagerState,
    uri: String,
    isRtl: Boolean,
    gestureState: ReaderGestureState,
    tapToNavigate: Boolean = false,
    intendedPage: Int = 0,
    pageCount: Int = 0,
    onIntendedPageChange: (Int) -> Unit = {},
    onNavigateByTap: (Int) -> Unit = {},
    isNavigatingByTap: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val coroutineScope = rememberCoroutineScope()

    HorizontalPager(
        state = pagerState,
        reverseLayout = isRtl,
        beyondViewportPageCount = 1,
        userScrollEnabled = !gestureState.isZoomed && !isNavigatingByTap,
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
                .pointerInput(tapToNavigate) {
                    detectTapGestures(
                        onTap = { offset ->
                            TapHandler(
                                tapToNavigate = tapToNavigate,
                                isZoomed = gestureState.isZoomed,
                                isRtl = isRtl,
                                currentPage = intendedPage,
                                pageCount = pageCount,
                            ).handle(
                                offset = offset,
                                containerWidth = size.width.toFloat(),
                                toggleBars = gestureState::toggleBars,
                                navigateToPage = { target ->
                                    onIntendedPageChange(target)
                                    onNavigateByTap(target)
                                }
                            )
                        },
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
                .pageZoom(
                    isCurrentPage = pageIndex == pagerState.currentPage,
                    gestureState = gestureState,
                )
        )
    }
}
