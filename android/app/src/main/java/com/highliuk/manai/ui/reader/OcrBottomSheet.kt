package com.highliuk.manai.ui.reader

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.os.LocaleList
import android.util.TypedValue
import android.view.ViewGroup
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.highliuk.manai.R
import com.highliuk.manai.domain.model.PageRegion
import java.util.Locale
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val SWIPE_DISMISS_THRESHOLD = 150f
private const val EXIT_ANIMATION_DURATION = 200L

@Composable
fun OcrBottomSheet(
    region: PageRegion,
    fontScale: Float = 1.5f,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    var visible by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) { visible = true }

    val animatedDismiss: () -> Unit = remember {
        {
            scope.launch {
                visible = false
                delay(EXIT_ANIMATION_DURATION)
                onDismiss()
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter,
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(durationMillis = 250)),
            exit = fadeOut(tween(durationMillis = 200)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.32f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = animatedDismiss,
                    ),
            )
        }

        var dragOffset by remember { mutableFloatStateOf(0f) }
        val draggableState = rememberDraggableState { delta ->
            dragOffset = (dragOffset + delta).coerceAtLeast(0f)
        }

        AnimatedVisibility(
            visible = visible,
            enter = slideInVertically(tween(durationMillis = 300)) { it },
            exit = slideOutVertically(tween(durationMillis = 200)) { it },
        ) {
            Surface(
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                tonalElevation = 1.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .offset { IntOffset(0, dragOffset.roundToInt()) }
                    .draggable(
                        state = draggableState,
                        orientation = Orientation.Vertical,
                        onDragStopped = {
                            if (dragOffset > SWIPE_DISMISS_THRESHOLD) {
                                animatedDismiss()
                            } else {
                                dragOffset = 0f
                            }
                        },
                    ),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .padding(horizontal = 24.dp, vertical = 16.dp)
                        .testTag("ocr_sheet_content"),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    if (region.ocrText == null) {
                        CircularProgressIndicator()
                        Text(
                            text = stringResource(R.string.ocr_loading),
                            modifier = Modifier.padding(top = 12.dp),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    } else {
                        val textColor = MaterialTheme.colorScheme.onSurface
                        val textSizeSp =
                            (MaterialTheme.typography.bodyLarge.fontSize * fontScale).value

                        AndroidView(
                            factory = { ctx ->
                                SelectableOcrTextView(ctx.findActivity() ?: ctx).apply {
                                    id = android.view.View.generateViewId()
                                    setTextIsSelectable(true)
                                    setLineSpacing(0f, 1.5f)
                                    textLocales = LocaleList(Locale("ja"))
                                    layoutParams = ViewGroup.LayoutParams(
                                        ViewGroup.LayoutParams.MATCH_PARENT,
                                        ViewGroup.LayoutParams.WRAP_CONTENT,
                                    )
                                }
                            },
                            update = { view ->
                                view.text = region.ocrText
                                view.setTextColor(textColor.toArgb())
                                view.setTextSize(
                                    TypedValue.COMPLEX_UNIT_SP,
                                    textSizeSp,
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("ocr_text")
                                .semantics {
                                    text = AnnotatedString(region.ocrText.orEmpty())
                                },
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp),
                            horizontalArrangement = Arrangement.End,
                        ) {
                            IconButton(onClick = {
                                val clipboard =
                                    context.getSystemService(Context.CLIPBOARD_SERVICE)
                                        as ClipboardManager
                                clipboard.setPrimaryClip(
                                    ClipData.newPlainText("OCR", region.ocrText)
                                )
                            }) {
                                Icon(
                                    Icons.Default.ContentCopy,
                                    contentDescription = stringResource(R.string.copy_text),
                                )
                            }
                            IconButton(onClick = {
                                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                    putExtra(Intent.EXTRA_TEXT, region.ocrText)
                                    type = "text/plain"
                                }
                                context.startActivity(
                                    Intent.createChooser(sendIntent, null)
                                )
                            }) {
                                Icon(
                                    Icons.Default.Share,
                                    contentDescription = stringResource(R.string.share_text),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun Context.findActivity(): Activity? {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}
