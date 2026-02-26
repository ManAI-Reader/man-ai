package com.highliuk.manai.ui.reader

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.semantics
import com.highliuk.manai.R
import com.highliuk.manai.domain.model.Manga
import com.highliuk.manai.domain.model.ReadingMode
import com.highliuk.manai.ui.navigation.LocalAnimatedVisibilityScope
import com.highliuk.manai.ui.navigation.LocalSharedTransitionScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

private const val DOUBLE_TAP_ANIM_DURATION = 300

internal val ZoomScaleKey = SemanticsPropertyKey<Float>("ZoomScale")
internal var SemanticsPropertyReceiver.zoomScale by ZoomScaleKey

private fun Modifier.pageZoom(
    isCurrentPage: Boolean,
    gestureState: ReaderGestureState,
): Modifier = this
    .semantics { zoomScale = if (isCurrentPage) gestureState.scale else 1f }
    .graphicsLayer {
        scaleX = if (isCurrentPage) gestureState.scale else 1f
        scaleY = if (isCurrentPage) gestureState.scale else 1f
        translationX = if (isCurrentPage) gestureState.offsetX else 0f
        translationY = if (isCurrentPage) gestureState.offsetY else 0f
    }

internal data class TapHandler(
    val tapToNavigate: Boolean,
    val isZoomed: Boolean,
    val isRtl: Boolean,
    val currentPage: Int,
    val pageCount: Int,
) {
    fun handle(
        offset: Offset,
        containerWidth: Float,
        toggleBars: () -> Unit,
        navigateToPage: (Int) -> Unit,
    ) {
        val delta = computePageDelta(offset.x, containerWidth)
        if (delta == 0) {
            toggleBars()
        } else {
            val target = (currentPage + delta).coerceIn(0, pageCount - 1)
            if (target == currentPage) {
                toggleBars()
            } else {
                navigateToPage(target)
            }
        }
    }

    private fun computePageDelta(tapX: Float, containerWidth: Float): Int {
        if (!tapToNavigate || isZoomed) return 0
        return when (classifyTapZone(tapX, containerWidth)) {
            TapZone.LEFT -> if (isRtl) 1 else -1
            TapZone.RIGHT -> if (isRtl) -1 else 1
            TapZone.CENTER -> 0
        }
    }
}

@Suppress("LongParameterList")
@OptIn(
    ExperimentalMaterial3Api::class,
    androidx.compose.foundation.ExperimentalFoundationApi::class,
    ExperimentalSharedTransitionApi::class
)
@Composable
fun ReaderScreen(
    manga: Manga,
    currentPage: Int,
    readingMode: ReadingMode = ReadingMode.LTR,
    tapToNavigate: Boolean = false,
    onPageChanged: (Int) -> Unit,
    onBack: () -> Unit,
    onSettingsClick: () -> Unit,
    onImmersiveModeChange: (Boolean) -> Unit = {},
) {
    val isRtl = readingMode == ReadingMode.RTL
    val pagerState = rememberPagerState(initialPage = currentPage) { manga.pageCount }
    val gestureState = remember { ReaderGestureState() }
    val coroutineScope = rememberCoroutineScope()
    var intendedPage by remember { mutableIntStateOf(currentPage) }
    var isNavigatingByTap by remember { mutableStateOf(false) }
    var navigationJob by remember { mutableStateOf<Job?>(null) }
    var showGoToPageDialog by remember { mutableStateOf(false) }

    val sharedTransitionScope = LocalSharedTransitionScope.current
    val animatedVisibilityScope = LocalAnimatedVisibilityScope.current

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { page ->
            intendedPage = page
            gestureState.resetZoom()
            onPageChanged(page)
        }
    }

    LaunchedEffect(gestureState.areBarsVisible) {
        onImmersiveModeChange(!gestureState.areBarsVisible)
    }

    DisposableEffect(Unit) {
        onDispose { onImmersiveModeChange(false) }
    }

    val sharedModifier = if (sharedTransitionScope != null && animatedVisibilityScope != null) {
        with(sharedTransitionScope) {
            Modifier.sharedBounds(
                sharedContentState = rememberSharedContentState(key = "manga_cover_${manga.id}"),
                animatedVisibilityScope = animatedVisibilityScope,
                enter = fadeIn(),
                exit = fadeOut(),
                resizeMode = SharedTransitionScope.ResizeMode.RemeasureToBounds
            )
        }
    } else {
        Modifier
    }

    Box(
        modifier = sharedModifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        HorizontalPager(
            state = pagerState,
            reverseLayout = isRtl,
            beyondViewportPageCount = 1,
            userScrollEnabled = !gestureState.isZoomed && !isNavigatingByTap,
            modifier = Modifier
                .fillMaxSize()
                .testTag("reader_pager")
        ) { pageIndex ->
            PdfPage(
                uri = manga.uri,
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
                                    pageCount = manga.pageCount,
                                ).handle(
                                    offset = offset,
                                    containerWidth = size.width.toFloat(),
                                    toggleBars = gestureState::toggleBars,
                                    navigateToPage = { target ->
                                        intendedPage = target
                                        navigationJob?.cancel()
                                        navigationJob = coroutineScope.launch {
                                            isNavigatingByTap = true
                                            try {
                                                pagerState.animateScrollToPage(target)
                                            } finally {
                                                isNavigatingByTap = false
                                            }
                                        }
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

        AnimatedVisibility(
            visible = gestureState.areBarsVisible,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                title = { Text(manga.title) },
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = stringResource(R.string.reader_settings)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }

        AnimatedVisibility(
            visible = gestureState.areBarsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            ReaderBottomBar(
                currentPage = pagerState.currentPage,
                pageCount = manga.pageCount,
                isRtl = isRtl,
                onPageSelected = { page ->
                    coroutineScope.launch { pagerState.scrollToPage(page) }
                },
                onPageIndicatorClick = { showGoToPageDialog = true }
            )
        }

        if (showGoToPageDialog) {
            GoToPageDialog(
                onConfirm = { pageNumber ->
                    showGoToPageDialog = false
                    val targetPage = pageNumber - 1
                    coroutineScope.launch { pagerState.scrollToPage(targetPage) }
                },
                onDismiss = { showGoToPageDialog = false }
            )
        }
    }
}
