package com.highliuk.manai.ui.reader

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.highliuk.manai.R
import com.highliuk.manai.domain.model.Manga
import com.highliuk.manai.domain.model.ReadingMode
import com.highliuk.manai.ui.navigation.LocalAnimatedVisibilityScope
import com.highliuk.manai.ui.navigation.LocalSharedTransitionScope
import kotlinx.coroutines.launch

@Suppress("LongParameterList", "LongMethod")
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
    onPageChanged: (Int) -> Unit,
    onBack: () -> Unit,
    onSettingsClick: () -> Unit,
    onImmersiveModeChange: (Boolean) -> Unit = {},
) {
    val isRtl = readingMode == ReadingMode.RTL
    val isWebtoon = readingMode == ReadingMode.WEBTOON
    val pagerState = rememberPagerState(initialPage = currentPage) { manga.pageCount }
    val lazyListState = rememberLazyListState(initialFirstVisibleItemIndex = currentPage)
    val gestureState = remember { ReaderGestureState() }
    val coroutineScope = rememberCoroutineScope()
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

    val displayedCurrentPage = if (isWebtoon) {
        computeWebtoonCurrentPage(
            firstVisibleItemIndex = lazyListState.firstVisibleItemIndex,
            canScrollForward = lazyListState.canScrollForward,
            lastVisibleItemIndex = lazyListState.layoutInfo.visibleItemsInfo
                .lastOrNull()?.index,
        )
    } else {
        pagerState.currentPage
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
            )
        } else {
            HorizontalPagerViewer(
                pagerState = pagerState,
                uri = manga.uri,
                isRtl = isRtl,
                gestureState = gestureState,
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
