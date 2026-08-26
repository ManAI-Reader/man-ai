package com.highliuk.manai.ui.reader

import android.content.Context
import android.text.Selection
import android.text.Spannable
import android.view.ActionMode
import android.view.GestureDetector
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.ViewConfiguration
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

    /**
     * When true the selection action mode shows ONLY the configured prompts:
     * no Copy/Select all/Share and no PROCESS_TEXT intents from other apps.
     * Entered by a single tap on a word; left when the user drags the
     * selection handles away from that word (or the action mode is destroyed).
     */
    private var promptOnlyMode = false
    private var promptOnlyWord: Pair<Int, Int>? = null
    private var activeActionMode: ActionMode? = null

    private var downX = 0f
    private var downY = 0f

    /**
     * Set when the current touch stream is not a single-tap candidate anymore:
     * second tap of a double tap, long press, or multi-pointer gesture. On a
     * double tap this keeps the second ACTION_UP from re-entering prompt-only
     * mode, so the double tap ends with the full menu.
     */
    private var suppressTapSelection = false

    private val touchSlopPx = ViewConfiguration.get(context).scaledTouchSlop.toFloat()

    private val gestureDetector = GestureDetector(
        context,
        object : GestureDetector.SimpleOnGestureListener() {
            override fun onDoubleTap(e: MotionEvent): Boolean {
                suppressTapSelection = true
                exitPromptOnlyMode()
                pendingWordSelection = true
                touchedOffset = getCharOffset(e)
                return false
            }

            override fun onLongPress(e: MotionEvent) {
                suppressTapSelection = true
                exitPromptOnlyMode()
                pendingWordSelection = true
                touchedOffset = getCharOffset(e)
            }
        },
    )

    init {
        customSelectionActionModeCallback = object : ActionMode.Callback {
            override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
                activeActionMode = mode
                return true
            }

            override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
                menuPrompts = selectionPrompts
                prepareSelectionMenu(menu, promptOnlyMode, menuPrompts)
                return true
            }

            override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
                val handled = item.groupId == PROMPT_GROUP && dispatchPromptClick(item.itemId)
                if (handled) mode.finish()
                return handled
            }

            override fun onDestroyActionMode(mode: ActionMode) {
                activeActionMode = null
                promptOnlyMode = false
            }
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
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.x
                downY = event.y
                suppressTapSelection = false
            }
            MotionEvent.ACTION_POINTER_DOWN -> suppressTapSelection = true
        }
        // May set suppressTapSelection: onDoubleTap fires on the second tap's
        // ACTION_DOWN, onLongPress fires while the finger is still down.
        gestureDetector.onTouchEvent(event)
        // Tap handling must happen INSIDE the touch stream, BEFORE super
        // consumes this ACTION_UP: performLongClick() makes the Editor enter
        // its word drag accelerator, and it is the native handling of this
        // very ACTION_UP that completes it (shows the selection handles and
        // starts the floating toolbar via startSelectionActionModeAsync).
        if (event.actionMasked == MotionEvent.ACTION_UP && !suppressTapSelection) {
            maybeHandleTapSelection(event)
        }
        return super.onTouchEvent(event)
    }

    private fun maybeHandleTapSelection(event: MotionEvent) {
        val isTap = isSingleTapGesture(
            durationMillis = event.eventTime - event.downTime,
            dxPx = event.x - downX,
            dyPx = event.y - downY,
            touchSlopPx = touchSlopPx,
            longPressTimeoutMillis = ViewConfiguration.getLongPressTimeout().toLong(),
        )
        if (isTap && isTapOnLaidOutText(event)) handleSingleTap(getCharOffset(event))
    }

    private fun isTapOnLaidOutText(e: MotionEvent): Boolean {
        val currentLayout = layout ?: return false
        val x = e.x - totalPaddingLeft + scrollX
        val y = e.y - totalPaddingTop + scrollY
        val line = currentLayout.getLineForVertical(y.toInt())
        return isTapOnText(
            xPx = x,
            yPx = y,
            line = TextLineBounds(
                leftPx = currentLayout.getLineLeft(line),
                rightPx = currentLayout.getLineRight(line),
                textBottomPx = currentLayout.getLineBottom(currentLayout.lineCount - 1).toFloat(),
            ),
            slopPx = touchSlopPx,
        )
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
        // A non-collapsed selection consumes the pending gesture even when no
        // adjustment is needed (native bounds already match the token).
        if (pendingWordSelection && selStart != selEnd) pendingWordSelection = false
        val spannable = text as? Spannable
        if (adjustment != null && spannable != null) {
            adjustingSelection = true
            Selection.setSelection(spannable, adjustment.first, adjustment.second)
            adjustingSelection = false
            return
        }
        maybeExitPromptOnlyMode(selStart, selEnd)
    }

    /**
     * Handles a single tap at char [offset]: selects the tapped word and opens
     * the native selection action mode in prompt-only mode. Returns false (no
     * selection, no menu) when the tap is outside any token, no prompts are
     * configured, or the native selection could not be started.
     */
    internal fun handleSingleTap(offset: Int): Boolean {
        val word = promptOnlyWordForTap(offset, furiganaTokens, selectionPrompts.isNotEmpty())
            ?: return false
        collapseActiveSelection(offset)
        promptOnlyWord = word
        promptOnlyMode = true
        pendingWordSelection = true
        touchedOffset = offset
        val started = startNativeSelection()
        if (!started) {
            promptOnlyWord = null
            promptOnlyMode = false
            pendingWordSelection = false
        }
        return started
    }

    /**
     * A long click landing on an active selection takes Editor's
     * drag-and-drop branch instead of selecting the tapped word; collapsing
     * the selection first guarantees the tap re-selects the tapped word.
     */
    private fun collapseActiveSelection(offset: Int) {
        val spannable = text as? Spannable ?: return
        if (selectionStart != selectionEnd) {
            Selection.setSelection(spannable, offset.coerceIn(0, spannable.length))
        }
    }

    /**
     * Reuses the native long-click path so the selection handles and floating
     * toolbar are the platform ones; haptics are suppressed because
     * TextView.performLongClick vibrates when handled, and a tap must not
     * feel like a long press. Returns whether the long click was handled.
     */
    private fun startNativeSelection(): Boolean {
        val hapticsWereEnabled = isHapticFeedbackEnabled
        isHapticFeedbackEnabled = false
        return try {
            performLongClick()
        } finally {
            isHapticFeedbackEnabled = hapticsWereEnabled
        }
    }

    private fun maybeExitPromptOnlyMode(selStart: Int, selEnd: Int) {
        val word = promptOnlyWord ?: return
        if (promptOnlyMode && shouldExitPromptOnlyMode(selStart, selEnd, word.first, word.second)) {
            exitPromptOnlyMode()
        }
    }

    private fun exitPromptOnlyMode() {
        if (!promptOnlyMode) return
        promptOnlyMode = false
        // Re-runs onPrepareActionMode, which rebuilds the full menu this time.
        activeActionMode?.invalidate()
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
        internal const val PROMPT_GROUP = 1001
        private const val MENU_ID_BASE = 2000
        private const val MENU_ORDER_BASE = 100

        fun promptMenuItemId(index: Int): Int = MENU_ID_BASE + index

        /**
         * Rebuilds the selection action-mode menu for the current mode.
         *
         * AOSP's Editor adds Cut/Copy/Share and the PROCESS_TEXT items only in
         * onCreateActionMode (PROCESS_TEXT after the custom create callback);
         * onPrepareActionMode re-adds only Select All/Replace/assist, and
         * invalidate() re-runs prepare alone. Clearing the menu would therefore
         * lose Copy/Share/PROCESS_TEXT for the rest of the action mode, so
         * prompt-only mode toggles visibility of non-prompt items instead.
         */
        fun prepareSelectionMenu(
            menu: Menu,
            promptOnly: Boolean,
            prompts: List<Pair<Long, String>>,
        ) {
            menu.removeGroup(PROMPT_GROUP)
            for (index in 0 until menu.size()) {
                val item = menu.getItem(index)
                if (item.groupId != PROMPT_GROUP) item.setVisible(!promptOnly)
            }
            prompts.forEachIndexed { index, (_, name) ->
                menu.add(PROMPT_GROUP, promptMenuItemId(index), index + MENU_ORDER_BASE, name)
            }
        }

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

        /**
         * Decides whether a completed touch (DOWN→UP) is a single-tap
         * candidate: released before the long-press timeout and with the
         * finger having moved at most the touch slop (euclidean distance).
         */
        fun isSingleTapGesture(
            durationMillis: Long,
            dxPx: Float,
            dyPx: Float,
            touchSlopPx: Float,
            longPressTimeoutMillis: Long,
        ): Boolean =
            durationMillis < longPressTimeoutMillis &&
                dxPx * dxPx + dyPx * dyPx <= touchSlopPx * touchSlopPx

        /**
         * Horizontal extent of the tapped line and bottom of the whole text,
         * in layout-relative pixels.
         */
        data class TextLineBounds(val leftPx: Float, val rightPx: Float, val textBottomPx: Float)

        /**
         * Decides whether a tap at layout-relative ([xPx], [yPx]) landed on
         * the laid-out text of its line rather than on empty space: vertical
         * offsets clamp to the nearest line and horizontal offsets clamp to
         * the nearest character, so without this guard a tap below the last
         * line or right of a wrapped line would resolve to a real character.
         * The native long press has the same guard (Editor.isPositionOnText).
         */
        fun isTapOnText(xPx: Float, yPx: Float, line: TextLineBounds, slopPx: Float): Boolean =
            yPx >= -slopPx && yPx <= line.textBottomPx + slopPx &&
                xPx >= line.leftPx - slopPx && xPx <= line.rightPx + slopPx

        /**
         * Decides whether a single tap at [offset] starts a prompt-only word
         * selection: inside a token → that token's bounds, outside any token
         * (e.g. past the end of the text) → null, meaning no selection and no
         * menu. With no prompts configured ([hasPrompts] false) the prompt-only
         * toolbar would be empty, so the tap never starts a selection.
         */
        fun promptOnlyWordForTap(
            offset: Int,
            tokens: List<FuriganaToken>,
            hasPrompts: Boolean,
        ): Pair<Int, Int>? =
            if (hasPrompts) wordBoundaryFromTokens(offset, tokens) else null

        /**
         * Decides whether a selection change should leave prompt-only mode.
         * Leaving means the user moved the selection handles away from the
         * word selected by the single tap; a collapsed (cursor) selection is
         * not a handle drag and never exits.
         */
        fun shouldExitPromptOnlyMode(
            selStart: Int,
            selEnd: Int,
            wordStart: Int,
            wordEnd: Int,
        ): Boolean {
            if (selStart == selEnd) return false
            val start = minOf(selStart, selEnd)
            val end = maxOf(selStart, selEnd)
            return start != wordStart || end != wordEnd
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
