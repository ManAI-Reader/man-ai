package com.highliuk.manai.ui.reader

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import com.highliuk.manai.R
import com.highliuk.manai.domain.model.Manga
import com.highliuk.manai.domain.model.PagePipelineState
import com.highliuk.manai.domain.model.PageRegion
import com.highliuk.manai.domain.model.ReadingMode
import com.highliuk.manai.ui.navigation.LocalAnimatedVisibilityScope
import com.highliuk.manai.ui.navigation.LocalSharedTransitionScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

internal val ZoomScaleKey = SemanticsPropertyKey<Float>("ZoomScale")
internal var SemanticsPropertyReceiver.zoomScale by ZoomScaleKey

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

@Suppress("LongParameterList", "LongMethod", "CyclomaticComplexMethod", "UnusedParameter")
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
    regions: List<PageRegion> = emptyList(),
    selectedRegion: PageRegion? = null,
    tapToNavigate: Boolean = false,
    onPageChanged: (Int) -> Unit,
    onRegionTapped: (PageRegion) -> Unit = {},
    ocrFontScale: Float = 1.5f,
    onDismissBottomSheet: () -> Unit = {},
    onBack: () -> Unit,
    onSettingsClick: () -> Unit,
    onImmersiveModeChange: (Boolean) -> Unit = {},
    debugPipelineStates: Map<Int, PagePipelineState> = emptyMap(),
    visiblePagesRegions: Map<Int, List<PageRegion>> = emptyMap(),
    onVisiblePagesChanged: (List<Int>) -> Unit = {},
) {
    val isRtl = readingMode == ReadingMode.RTL
    val isWebtoon = readingMode == ReadingMode.WEBTOON
    val pagerState = rememberPagerState(initialPage = currentPage) { manga.pageCount }
    val lazyListState = rememberLazyListState(initialFirstVisibleItemIndex = currentPage)
    val gestureState = remember { ReaderGestureState() }
    val coroutineScope = rememberCoroutineScope()
    var intendedPage by remember { mutableIntStateOf(currentPage) }
    var isNavigatingByTap by remember { mutableStateOf(false) }
    var navigationJob by remember { mutableStateOf<Job?>(null) }
    var showGoToPageDialog by remember { mutableStateOf(false) }
    val sharedTransitionScope = LocalSharedTransitionScope.current
    val animatedVisibilityScope = LocalAnimatedVisibilityScope.current

    LaunchedEffect(readingMode) {
        if (isWebtoon) {
            lazyListState.scrollToItem(currentPage)
        } else {
            pagerState.scrollToPage(currentPage)
        }
        gestureState.resetZoom()
    }

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { page ->
            if (!isWebtoon) {
                intendedPage = page
                gestureState.resetZoom()
                onPageChanged(page)
            }
        }
    }

    LaunchedEffect(lazyListState, isWebtoon) {
        if (isWebtoon) {
            snapshotFlow {
                computeWebtoonCurrentPage(
                    firstVisibleItemIndex = lazyListState.firstVisibleItemIndex,
                    canScrollForward = lazyListState.canScrollForward,
                    lastVisibleItemIndex = lazyListState.layoutInfo.visibleItemsInfo
                        .lastOrNull()?.index,
                )
            }.collect { page ->
                onPageChanged(page)
            }
        }
    }

    LaunchedEffect(lazyListState, isWebtoon) {
        if (isWebtoon) {
            snapshotFlow {
                lazyListState.layoutInfo.visibleItemsInfo.map { it.index }
            }.collect { pages ->
                onVisiblePagesChanged(pages)
            }
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
                enter = fadeIn(tween(300)),
                exit = fadeOut(tween(300)),
                resizeMode = SharedTransitionScope.ResizeMode.RemeasureToBounds
            )
        }
    } else {
        Modifier
    }

    val displayedCurrentPage by remember(isWebtoon) {
        derivedStateOf {
            if (isWebtoon) {
                computeWebtoonCurrentPage(
                    firstVisibleItemIndex = lazyListState.firstVisibleItemIndex,
                    canScrollForward = lazyListState.canScrollForward,
                    lastVisibleItemIndex = lazyListState.layoutInfo.visibleItemsInfo
                        .lastOrNull()?.index,
                )
            } else {
                pagerState.currentPage
            }
        }
    }

    Box(
        modifier = sharedModifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (isWebtoon) {
            WebtoonViewer(
                lazyListState = lazyListState,
                uri = manga.uri,
                pageCount = manga.pageCount,
                gestureState = gestureState,
                visiblePagesRegions = visiblePagesRegions,
                onRegionTapped = onRegionTapped,
                debugPipelineStates = debugPipelineStates,
            )
        } else {
            HorizontalPagerViewer(
                pagerState = pagerState,
                uri = manga.uri,
                isRtl = isRtl,
                gestureState = gestureState,
                tapToNavigate = tapToNavigate,
                intendedPage = intendedPage,
                pageCount = manga.pageCount,
                onIntendedPageChange = { intendedPage = it },
                onNavigateByTap = { target ->
                    navigationJob?.cancel()
                    navigationJob = coroutineScope.launch {
                        isNavigatingByTap = true
                        try {
                            pagerState.animateScrollToPage(target)
                        } finally {
                            isNavigatingByTap = false
                        }
                    }
                },
                isNavigatingByTap = isNavigatingByTap,
                regions = regions,
                onRegionTapped = onRegionTapped,
                debugPipelineStates = debugPipelineStates,
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
                currentPage = displayedCurrentPage,
                pageCount = manga.pageCount,
                isRtl = if (isWebtoon) false else isRtl,
                onPageSelected = { page ->
                    coroutineScope.launch {
                        if (isWebtoon) {
                            lazyListState.scrollToItem(page)
                        } else {
                            pagerState.scrollToPage(page)
                        }
                    }
                },
                onPageIndicatorClick = { showGoToPageDialog = true }
            )
        }

        if (selectedRegion != null) {
            val sourceRegions = if (isWebtoon) {
                visiblePagesRegions[selectedRegion.pageIndex].orEmpty()
            } else {
                regions
            }
            val liveRegion = sourceRegions.find { it.regionIndex == selectedRegion.regionIndex }
                ?: selectedRegion
            OcrBottomSheet(region = liveRegion, fontScale = ocrFontScale, onDismiss = onDismissBottomSheet)
        }

        if (showGoToPageDialog) {
            GoToPageDialog(
                onConfirm = { pageNumber ->
                    showGoToPageDialog = false
                    val targetPage = (pageNumber - 1).coerceIn(0, manga.pageCount - 1)
                    coroutineScope.launch {
                        if (isWebtoon) {
                            lazyListState.scrollToItem(targetPage)
                        } else {
                            pagerState.scrollToPage(targetPage)
                        }
                    }
                },
                onDismiss = { showGoToPageDialog = false }
            )
        }
    }
}
