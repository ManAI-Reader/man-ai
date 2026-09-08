package com.highliuk.manai.ui.chat

import android.graphics.Canvas
import android.graphics.Paint
import android.text.Layout
import android.text.Spanned
import android.text.style.LeadingMarginSpan
import kotlin.math.ceil

internal fun listItemMarker(ordered: Boolean, index: Int): String =
    if (ordered) "$index." else "•"

internal fun listMarkerMarginPx(markerWidthPx: Float, gapPx: Float): Int =
    ceil(markerWidthPx + gapPx).toInt()

internal fun shouldDrawMarkerOnLine(spanStart: Int, lineStart: Int): Boolean =
    spanStart == lineStart

/**
 * Draws a list marker (bullet or ordinal) inside the leading margin of its
 * paragraph, with the paragraph's own paint and on the first line's
 * baseline. A marker rendered as a separate composable can never line up
 * with the TextView's first line: furigana rubies raise that line, so the
 * marker must live inside the same text layout.
 */
internal class ListMarkerSpan(
    private val marker: String,
    private val marginPx: Int,
) : LeadingMarginSpan {

    override fun getLeadingMargin(first: Boolean): Int = marginPx

    @Suppress("LongParameterList")
    override fun drawLeadingMargin(
        canvas: Canvas,
        paint: Paint,
        x: Int,
        dir: Int,
        top: Int,
        baseline: Int,
        bottom: Int,
        text: CharSequence,
        start: Int,
        end: Int,
        first: Boolean,
        layout: Layout,
    ) {
        val spanStart = (text as Spanned).getSpanStart(this)
        if (!shouldDrawMarkerOnLine(spanStart, start)) return
        val originX = if (dir >= 0) x.toFloat() else x - paint.measureText(marker)
        canvas.drawText(marker, originX, baseline.toFloat(), paint)
    }
}
