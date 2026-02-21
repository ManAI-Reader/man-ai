package com.highliuk.manai.ui.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Slider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.highliuk.manai.R
import kotlin.math.roundToInt

@Composable
@Suppress("LongParameterList")
fun ReaderBottomBar(
    currentPage: Int,
    pageCount: Int,
    isRtl: Boolean = false,
    onPageSelected: (Int) -> Unit,
    onPageIndicatorClick: () -> Unit = {},
) {
    var isDragging by remember { mutableStateOf(false) }
    var dragValue by remember { mutableFloatStateOf(currentPage.toFloat()) }

    val displayedPage = if (isDragging) dragValue.toInt() + 1 else currentPage + 1

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .testTag("reader_bottom_bar")
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f))
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = stringResource(R.string.page_indicator, displayedPage, pageCount),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .clickable(onClick = onPageIndicatorClick)
                .testTag("page_indicator")
        )

        if (pageCount > 1) {
            val layoutDirection = if (isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr
            val sliderTag = if (isRtl) "page_slider_rtl" else "page_slider"
            CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                Slider(
                    value = if (isDragging) dragValue else currentPage.toFloat(),
                    onValueChange = { value ->
                        isDragging = true
                        dragValue = value.roundToInt().toFloat()
                    },
                    onValueChangeFinished = {
                        isDragging = false
                        onPageSelected(dragValue.roundToInt())
                    },
                    valueRange = 0f..(pageCount - 1).toFloat(),
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.onSurface,
                        activeTrackColor = MaterialTheme.colorScheme.onSurface,
                        inactiveTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(sliderTag)
                )
            }
        }
    }
}
