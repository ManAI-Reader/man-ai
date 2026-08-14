package com.highliuk.manai.ui.reader

import android.content.Context
import android.text.Selection
import android.text.Spannable
import android.view.ActionMode
import android.view.GestureDetector
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.widget.TextView
import com.highliuk.manai.domain.model.FuriganaToken

class SelectableOcrTextView(context: Context) : TextView(context) {

    var furiganaTokens: List<FuriganaToken> = emptyList()

    /** Prompt templates shown in the text-selection action mode, as id-to-name pairs. */
    var selectionPrompts: List<Pair<Long, String>> = emptyList()
    var onPromptSelected: ((promptId: Long, selectedText: String) -> Unit)? = null

    /**
     * Snapshot of [selectionPrompts] taken when the action-mode menu is built.
     * Clicks resolve against this snapshot so a mutation of the live list while
     * the menu is open cannot dispatch the wrong prompt.
     */
    private var menuPrompts: List<Pair<Long, String>> = emptyList()

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

    init {
        customSelectionActionModeCallback = object : ActionMode.Callback {
            override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean = true

            override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
                menuPrompts = selectionPrompts
                menu.removeGroup(PROMPT_GROUP)
                menuPrompts.forEachIndexed { index, (_, name) ->
                    menu.add(PROMPT_GROUP, promptMenuItemId(index), index + MENU_ORDER_BASE, name)
                }
                return true
            }

            override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
                val handled = item.groupId == PROMPT_GROUP && dispatchPromptClick(item.itemId)
                if (handled) mode.finish()
                return handled
            }

            override fun onDestroyActionMode(mode: ActionMode) = Unit
        }
    }

    private fun dispatchPromptClick(itemId: Int): Boolean {
        val promptId = promptIdForMenuItem(itemId, menuPrompts)
        val bounds = safeSelection(selectionStart, selectionEnd, text.length)
        return if (promptId != null && bounds != null) {
            onPromptSelected?.invoke(promptId, text.substring(bounds.first, bounds.second))
            true
        } else {
            false
        }
    }

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
        val x = (e.x.toInt() - totalPaddingLeft + scrollX).toFloat()
        val y = e.y.toInt() - totalPaddingTop + scrollY
        val currentLayout = layout ?: return 0
        val line = currentLayout.getLineForVertical(y)
        val cursorOffset = currentLayout.getOffsetForHorizontal(line, x)
        val cursorOffsetX = currentLayout.getPrimaryHorizontal(cursorOffset)
        return resolveTappedCharOffset(cursorOffset, x, cursorOffsetX)
    }

    companion object {
        private const val PROMPT_GROUP = 1001
        private const val MENU_ID_BASE = 2000
        private const val MENU_ORDER_BASE = 100

        fun promptMenuItemId(index: Int): Int = MENU_ID_BASE + index

        fun promptIdForMenuItem(itemId: Int, prompts: List<Pair<Long, String>>): Long? =
            prompts.getOrNull(itemId - MENU_ID_BASE)?.first

        /**
         * Orders and clamps a raw selection range to [0, textLength].
         * Returns null when the resulting range is empty or invalid.
         */
        fun safeSelection(selStart: Int, selEnd: Int, textLength: Int): Pair<Int, Int>? {
            val start = minOf(selStart, selEnd).coerceIn(0, textLength)
            val end = maxOf(selStart, selEnd).coerceIn(0, textLength)
            return if (start < end) start to end else null
        }

        // `Layout.getOffsetForHorizontal` returns a cursor-insertion offset, not
        // the index of the glyph that was touched: a tap on the right half of a
        // char lands on the cursor position AFTER it. Map that back to the
        // tapped char by checking whether the tap is to the left of the cursor's
        // own left edge.
        fun resolveTappedCharOffset(
            cursorOffset: Int,
            tapX: Float,
            cursorOffsetX: Float,
        ): Int = if (cursorOffset > 0 && tapX < cursorOffsetX) cursorOffset - 1 else cursorOffset

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
