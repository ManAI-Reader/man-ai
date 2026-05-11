package com.highliuk.manai.ui.reader

import android.graphics.Canvas
import android.graphics.Paint
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ReplacementSpan
import com.highliuk.manai.domain.model.FuriganaPart

private const val RUBY_SCALE = 0.5f

class RubySpan(private val reading: String) : ReplacementSpan() {

    override fun getSize(
        paint: Paint,
        text: CharSequence,
        start: Int,
        end: Int,
        fm: Paint.FontMetricsInt?,
    ): Int {
        val baseWidth = paint.measureText(text, start, end)
        if (fm != null) {
            val rubyHeight = (paint.textSize * RUBY_SCALE).toInt()
            fm.ascent = paint.fontMetricsInt.ascent - rubyHeight
            fm.top = paint.fontMetricsInt.top - rubyHeight
            fm.descent = paint.fontMetricsInt.descent
            fm.bottom = paint.fontMetricsInt.bottom
        }
        return baseWidth.toInt()
    }

    override fun draw(
        canvas: Canvas,
        text: CharSequence,
        start: Int,
        end: Int,
        x: Float,
        top: Int,
        y: Int,
        bottom: Int,
        paint: Paint,
    ) {
        val baseText = text.subSequence(start, end).toString()
        val baseWidth = paint.measureText(baseText)
        canvas.drawText(baseText, x, y.toFloat(), paint)

        val rubyPaint = Paint(paint).apply {
            textSize = paint.textSize * RUBY_SCALE
        }
        val rubyWidth = rubyPaint.measureText(reading)
        val rubyY = y + paint.fontMetricsInt.ascent.toFloat()
        val scaleX = minOf(1f, baseWidth / rubyWidth)
        canvas.save()
        canvas.scale(scaleX, 1f, x + baseWidth / 2, rubyY)
        val rubyX = x + (baseWidth - rubyWidth) / 2
        canvas.drawText(reading, rubyX, rubyY, rubyPaint)
        canvas.restore()
    }
}

data class SpanInstruction(val start: Int, val end: Int, val reading: String)

fun calculateSpanInstructions(parts: List<FuriganaPart>): List<SpanInstruction> {
    val instructions = mutableListOf<SpanInstruction>()
    var offset = 0
    for (part in parts) {
        if (part.reading != null) {
            instructions.add(SpanInstruction(offset, offset + part.surface.length, part.reading))
        }
        offset += part.surface.length
    }
    return instructions
}

fun buildFuriganaSpannable(parts: List<FuriganaPart>): SpannableStringBuilder {
    val builder = SpannableStringBuilder()
    for (part in parts) {
        val start = builder.length
        builder.append(part.surface)
        if (part.reading != null) {
            builder.setSpan(
                RubySpan(part.reading),
                start,
                builder.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
            )
        }
    }
    return builder
}
