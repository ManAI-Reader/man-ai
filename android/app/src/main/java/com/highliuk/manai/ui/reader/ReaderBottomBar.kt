package com.highliuk.manai.ui.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.highliuk.manai.R
import kotlin.math.roundToInt

@Composable
fun ReaderBottomBar(
    currentPage: Int,
    pageCount: Int,
    onPageSelected: (Int) -> Unit
) {
    var isDragging by remember { mutableStateOf(false) }
    var dragValue by remember { mutableFloatStateOf(currentPage.toFloat()) }

    val displayedPage = if (isDragging) dragValue.toInt() + 1 else currentPage + 1

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.7f))
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = stringResource(R.string.page_indicator, displayedPage, pageCount),
            color = Color.White
        )

        if (pageCount > 1) {
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
                    thumbColor = Color.White,
                    activeTrackColor = Color.White,
                    inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("page_slider")
            )
        }
    }
}
