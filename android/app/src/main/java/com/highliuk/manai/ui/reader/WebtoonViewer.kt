package com.highliuk.manai.ui.reader

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.platform.testTag
import com.highliuk.manai.domain.model.PagePipelineState
import com.highliuk.manai.domain.model.PageRegion
import kotlinx.coroutines.launch

private const val DOUBLE_TAP_ANIM_DURATION = 300

@Suppress("LongMethod", "LongParameterList")
@Composable
fun WebtoonViewer(
    lazyListState: LazyListState,
    uri: String,
    pageCount: Int,
    gestureState: ReaderGestureState,
    modifier: Modifier = Modifier,
    visiblePagesRegions: Map<Int, List<PageRegion>> = emptyMap(),
    onRegionTapped: (PageRegion) -> Unit = {},
    debugPipelineStates: Map<Int, PagePipelineState> = emptyMap(),
) {
    val coroutineScope = rememberCoroutineScope()

    LazyColumn(
        state = lazyListState,
        userScrollEnabled = true,
        modifier = modifier
            .fillMaxSize()
            .testTag("webtoon_viewer")
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
                translationY = gestureState.offsetY
            },
    ) {
        items(pageCount) { pageIndex ->
            val regions = visiblePagesRegions[pageIndex].orEmpty()
            var bitmapWidth by remember { mutableIntStateOf(0) }
            var bitmapHeight by remember { mutableIntStateOf(0) }

            Box(modifier = Modifier.fillMaxWidth()) {
                PdfPage(
                    uri = uri,
                    pageIndex = pageIndex,
                    onBitmapLoaded = { w, h ->
                        bitmapWidth = w
                        bitmapHeight = h
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("webtoon_page_$pageIndex")
                        .pointerInput(regions) {
                            detectTapGestures(
                                onTap = { offset ->
                                    val regionHit = if (regions.isNotEmpty()) {
                                        val bw = bitmapWidth.takeIf { it > 0 } ?: size.width
                                        val bh = bitmapHeight.takeIf { it > 0 } ?: size.height
                                        RegionHitTester.hitTest(
                                            tapX = offset.x, tapY = offset.y,
                                            regions = regions,
                                            containerWidth = size.width.toFloat(),
                                            containerHeight = size.height.toFloat(),
                                            bitmapWidth = bw, bitmapHeight = bh,
                                            scale = 1f, offsetX = 0f, offsetY = 0f,
                                        )
                                    } else {
                                        null
                                    }
                                    if (regionHit != null) {
                                        onRegionTapped(regionHit)
                                    } else {
                                        gestureState.toggleBars()
                                    }
                                },
                                onDoubleTap = { offset ->
                                    val viewportHeight =
                                        lazyListState.layoutInfo.viewportSize.height.toFloat()
                                    val pageOffset = lazyListState.layoutInfo.visibleItemsInfo
                                        .find { it.index == pageIndex }?.offset?.toFloat() ?: 0f
                                    val target = gestureState.onDoubleTapWebtoon(
                                        tapX = offset.x,
                                        tapY = pageOffset + offset.y,
                                        containerWidth = size.width.toFloat(),
                                        containerHeight = viewportHeight
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
                                                    scale = startScale +
                                                        (target.scale - startScale) * progress,
                                                    offsetX = startOffsetX +
                                                        (target.offsetX - startOffsetX) * progress,
                                                    offsetY = startOffsetY +
                                                        (target.offsetY - startOffsetY) * progress
                                                )
                                            )
                                        }
                                    }
                                }
                            )
                        },
                )
                if (debugPipelineStates.isNotEmpty()) {
                    DebugMlOverlay(
                        pageState = debugPipelineStates[pageIndex],
                        regions = regions,
                        bitmapWidth = bitmapWidth,
                        bitmapHeight = bitmapHeight,
                        modifier = Modifier.matchParentSize(),
                    )
                }
            }
        }
    }
}
