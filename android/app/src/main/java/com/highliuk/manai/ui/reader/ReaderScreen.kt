package com.highliuk.manai.ui.reader

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.highliuk.manai.R
import com.highliuk.manai.domain.model.Manga
import com.highliuk.manai.domain.model.ReadingMode
import kotlinx.coroutines.launch

@Suppress("LongParameterList")
@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun ReaderScreen(
    manga: Manga,
    currentPage: Int,
    readingMode: ReadingMode = ReadingMode.LTR,
    onPageChanged: (Int) -> Unit,
    onBack: () -> Unit,
    onSettingsClick: () -> Unit,
) {
    val isRtl = readingMode == ReadingMode.RTL
    val pagerState = rememberPagerState(initialPage = currentPage) { manga.pageCount }
    val gestureState = remember { ReaderGestureState() }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { page ->
            gestureState.resetZoom()
            onPageChanged(page)
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        HorizontalPager(
            state = pagerState,
            reverseLayout = isRtl,
            userScrollEnabled = !gestureState.isZoomed,
            modifier = Modifier
                .fillMaxSize()
                .testTag("reader_pager")
        ) { pageIndex ->
            PdfPage(
                uri = manga.uri,
                pageIndex = pageIndex,
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("reader_zoom_container")
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = { gestureState.toggleBars() }
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
                    containerColor = Color.Black.copy(alpha = 0.7f),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
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
                }
            )
        }
    }
}
