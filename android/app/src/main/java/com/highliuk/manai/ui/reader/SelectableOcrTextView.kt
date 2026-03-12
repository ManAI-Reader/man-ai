package com.highliuk.manai.ui.reader

import android.content.Context
import android.text.Selection
import android.text.Spannable
import android.view.GestureDetector
import android.view.MotionEvent
import android.widget.TextView

class SelectableOcrTextView(context: Context) : TextView(context) {

    private val gestureDetector = GestureDetector(
        context,
        object : GestureDetector.SimpleOnGestureListener() {
            override fun onDoubleTap(e: MotionEvent): Boolean {
                post { applyMockWordSelection(e) }
                return false
            }

            override fun onLongPress(e: MotionEvent) {
                post { applyMockWordSelection(e) }
            }
        },
    )

    @Suppress("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        gestureDetector.onTouchEvent(event)
        return super.onTouchEvent(event)
    }

    private fun applyMockWordSelection(e: MotionEvent) {
        val offset = getCharOffset(e)
        val (start, end) = mockWordBoundary(offset, text.length)
        val spannable = text as? Spannable ?: return
        Selection.setSelection(spannable, start, end)
    }

    private fun getCharOffset(e: MotionEvent): Int {
        val x = e.x.toInt() - totalPaddingLeft + scrollX
        val y = e.y.toInt() - totalPaddingTop + scrollY
        val currentLayout = layout ?: return 0
        val line = currentLayout.getLineForVertical(y)
        return currentLayout.getOffsetForHorizontal(line, x.toFloat())
    }

    companion object {
        fun mockWordBoundary(offset: Int, textLength: Int): Pair<Int, Int> {
            val start = (offset - 1).coerceAtLeast(0)
            val end = (offset + 2).coerceAtMost(textLength)
            return start to end
        }
    }
}
