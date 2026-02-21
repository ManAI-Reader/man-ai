package com.highliuk.manai.ui.reader

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.highliuk.manai.R

@Composable
fun PdfPage(
    uri: String,
    pageIndex: Int,
    modifier: Modifier = Modifier,
    onBitmapLoaded: ((width: Int, height: Int) -> Unit)? = null
) {
    val context = LocalContext.current
    val bitmap = produceState<Bitmap?>(initialValue = null, uri, pageIndex) {
        value = try {
            val pfd = context.contentResolver.openFileDescriptor(Uri.parse(uri), "r")
            pfd?.use { fd ->
                PdfRenderer(fd).use { renderer ->
                    if (pageIndex < renderer.pageCount) {
                        renderer.openPage(pageIndex).use { page ->
                            val bmp = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
                            page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                            bmp
                        }
                    } else null
                }
            }
        } catch (_: Exception) {
            null
        }
    }

    LaunchedEffect(bitmap.value) {
        bitmap.value?.let { bmp ->
            onBitmapLoaded?.invoke(bmp.width, bmp.height)
        }
    }

    if (bitmap.value != null) {
        Image(
            bitmap = bitmap.value!!.asImageBitmap(),
            contentDescription = stringResource(R.string.page_content, pageIndex + 1),
            modifier = modifier,
            contentScale = ContentScale.FillWidth
        )
    } else {
        Box(
            modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.PictureAsPdf,
                contentDescription = stringResource(R.string.pdf_placeholder),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
