package com.highliuk.manai.ui.reader

import android.content.Context
import android.text.Selection
import android.text.Spannable
import android.view.GestureDetector
import android.view.MotionEvent
import android.widget.TextView
import com.highliuk.manai.domain.model.FuriganaToken

class SelectableOcrTextView(context: Context) : TextView(context) {

    var furiganaTokens: List<FuriganaToken> = emptyList()

    private var pendingWordSelection = false
    private var touchedOffset = 0
    private var adjustingSelection = false

    private val gestureDetector = GestureDetector(
        context,
        object : GestureDetector.SimpleOnGestureListener() {
            override fun onDoubleTap(e: MotionEvent): Boolean {
                pendingWordSelection = true
                touchedOffset = getCharOffset(e)
                return false
            }

            override fun onLongPress(e: MotionEvent) {
                pendingWordSelection = true
                touchedOffset = getCharOffset(e)
            }
        },
    )

    @Suppress("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        gestureDetector.onTouchEvent(event)
        return super.onTouchEvent(event)
    }

    override fun onSelectionChanged(selStart: Int, selEnd: Int) {
        super.onSelectionChanged(selStart, selEnd)
        if (adjustingSelection) return
        val adjustment = computeWordSelectionAdjustment(
            pending = pendingWordSelection,
            touchedOffset = touchedOffset,
            nativeSelStart = selStart,
            nativeSelEnd = selEnd,
            tokens = furiganaTokens,
        )
        val spannable = text as? Spannable
        if (adjustment != null && spannable != null) {
            pendingWordSelection = false
            adjustingSelection = true
            Selection.setSelection(spannable, adjustment.first, adjustment.second)
            adjustingSelection = false
        }
    }

    private fun getCharOffset(e: MotionEvent): Int {
        val x = e.x.toInt() - totalPaddingLeft + scrollX
        val y = e.y.toInt() - totalPaddingTop + scrollY
        val currentLayout = layout ?: return 0
        val line = currentLayout.getLineForVertical(y)
        return currentLayout.getOffsetForHorizontal(line, x.toFloat())
    }

    companion object {
        fun wordBoundaryFromTokens(
            offset: Int,
            tokens: List<FuriganaToken>,
        ): Pair<Int, Int>? {
            var cursor = 0
            for (token in tokens) {
                val end = cursor + token.surface.length
                if (offset in cursor until end) {
                    return cursor to end
                }
                cursor = end
            }
            return null
        }

        fun computeWordSelectionAdjustment(
            pending: Boolean,
            touchedOffset: Int,
            nativeSelStart: Int,
            nativeSelEnd: Int,
            tokens: List<FuriganaToken>,
        ): Pair<Int, Int>? {
            if (!pending || nativeSelStart == nativeSelEnd) return null
            val boundary = wordBoundaryFromTokens(touchedOffset, tokens)
            return boundary?.takeUnless { it.first == nativeSelStart && it.second == nativeSelEnd }
        }
    }
}
