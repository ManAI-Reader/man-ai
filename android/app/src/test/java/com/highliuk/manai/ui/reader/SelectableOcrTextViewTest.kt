package com.highliuk.manai.ui.reader

import com.highliuk.manai.domain.model.FuriganaPart
import com.highliuk.manai.domain.model.FuriganaToken
import org.junit.Assert.assertEquals
import org.junit.Test

class SelectableOcrTextViewTest {

    // "私は食べる" — tokens: 私[0..1), は[1..2), 食べる[2..5)
    private val sampleTokens = listOf(
        FuriganaToken(
            surface = "私",
            reading = "わたし",
            parts = listOf(FuriganaPart.kanji("私", "わたし")),
        ),
        FuriganaToken(
            surface = "は",
            reading = null,
            parts = listOf(FuriganaPart.kana("は")),
        ),
        FuriganaToken(
            surface = "食べる",
            reading = "たべる",
            parts = listOf(
                FuriganaPart.kanji("食", "た"),
                FuriganaPart.kana("べる"),
            ),
        ),
    )

    // --- resolveTappedCharOffset tests ---

    @Test
    fun tappedCharOffsetReturnsPreviousCharWhenTapFallsOnItsRightHalf() {
        // Layout has chars A[0..10), B[10..20), C[20..30), D[30..40).
        // Tap on right half of B (tapX=18) → Android's getOffsetForHorizontal
        // returns cursor offset 2 (cursor after B). The right edge of B
        // (= left edge of C = getPrimaryHorizontal(2)) is 20.
        // The actually-tapped char is B (index 1), not C (index 2).
        val result = SelectableOcrTextView.resolveTappedCharOffset(
            cursorOffset = 2,
            tapX = 18f,
            cursorOffsetX = 20f,
        )
        assertEquals(1, result)
    }

    @Test
    fun tappedCharOffsetReturnsCursorOffsetWhenTapFallsOnLeftHalf() {
        // Tap on left half of B (tapX=11). Cursor offset = 1 (cursor before B),
        // cursorOffsetX = getPrimaryHorizontal(1) = 10. Tap is to the right of
        // that edge → tapped char is B (index 1).
        val result = SelectableOcrTextView.resolveTappedCharOffset(
            cursorOffset = 1,
            tapX = 11f,
            cursorOffsetX = 10f,
        )
        assertEquals(1, result)
    }

    @Test
    fun tappedCharOffsetReturnsZeroWhenCursorOffsetIsZero() {
        // Tap on first char's left half: cursor offset clamps to 0.
        val result = SelectableOcrTextView.resolveTappedCharOffset(
            cursorOffset = 0,
            tapX = 2f,
            cursorOffsetX = 0f,
        )
        assertEquals(0, result)
    }

    @Test
    fun tappedCharOffsetReturnsLastCharWhenTapOnRightHalfOfLastChar() {
        // 4-char text, tap on right half of D (tapX=38). Cursor offset = 4
        // (cursor after D), cursorOffsetX = getPrimaryHorizontal(4) = 40.
        // Tapped char is D (index 3).
        val result = SelectableOcrTextView.resolveTappedCharOffset(
            cursorOffset = 4,
            tapX = 38f,
            cursorOffsetX = 40f,
        )
        assertEquals(3, result)
    }

    // --- computeWordSelectionAdjustment tests ---

    @Test
    fun adjustmentReturnsNullWhenNotPending() {
        val result = SelectableOcrTextView.computeWordSelectionAdjustment(
            pending = false,
            touchedOffset = 3,
            nativeSelStart = 0,
            nativeSelEnd = 5,
            tokens = sampleTokens,
        )
        assertEquals(null, result)
    }

    @Test
    fun adjustmentReturnsNullForCursorSelection() {
        val result = SelectableOcrTextView.computeWordSelectionAdjustment(
            pending = true,
            touchedOffset = 3,
            nativeSelStart = 3,
            nativeSelEnd = 3,
            tokens = sampleTokens,
        )
        assertEquals(null, result)
    }

    @Test
    fun adjustmentSelectsWholeTokenWhenTappingFirstCharOfMultiCharToken() {
        // Tap on 食 (offset 2) — first char of 食べる.
        // The whole token "食べる" must be selected → (2, 5).
        val result = SelectableOcrTextView.computeWordSelectionAdjustment(
            pending = true,
            touchedOffset = 2,
            nativeSelStart = 0,
            nativeSelEnd = 5,
            tokens = sampleTokens,
        )

        assertEquals(2 to 5, result)
    }

    @Test
    fun adjustmentSelectsSingleCharTokenAtStart() {
        // Tap on 私 (offset 0) — single-char token → (0, 1).
        val result = SelectableOcrTextView.computeWordSelectionAdjustment(
            pending = true,
            touchedOffset = 0,
            nativeSelStart = 0,
            nativeSelEnd = 5,
            tokens = sampleTokens,
        )

        assertEquals(0 to 1, result)
    }

    @Test
    fun adjustmentReturnsNullWhenBoundaryMatchesNative() {
        // Tap on 食 (offset 2) → token boundary (2, 5).
        // Native already at (2, 5), no adjustment needed.
        val result = SelectableOcrTextView.computeWordSelectionAdjustment(
            pending = true,
            touchedOffset = 2,
            nativeSelStart = 2,
            nativeSelEnd = 5,
            tokens = sampleTokens,
        )
        assertEquals(null, result)
    }

    @Test
    fun adjustmentReturnsNullWhenTokensEmpty() {
        // No tokens → no word boundary → no adjustment.
        val result = SelectableOcrTextView.computeWordSelectionAdjustment(
            pending = true,
            touchedOffset = 3,
            nativeSelStart = 0,
            nativeSelEnd = 5,
            tokens = emptyList(),
        )
        assertEquals(null, result)
    }

    // --- wordBoundaryFromTokens tests ---

    @Test
    fun wordBoundaryReturnsTokenRangeForTapInsideToken() {
        // Tap on べ (offset 3) → inside 食べる → (2, 5).
        val result = SelectableOcrTextView.wordBoundaryFromTokens(
            offset = 3,
            tokens = sampleTokens,
        )
        assertEquals(2 to 5, result)
    }

    @Test
    fun wordBoundaryReturnsSecondTokenAtItsStart() {
        // Tap at offset 1 — start of は (boundary belongs to next token).
        val result = SelectableOcrTextView.wordBoundaryFromTokens(
            offset = 1,
            tokens = sampleTokens,
        )
        assertEquals(1 to 2, result)
    }

    @Test
    fun wordBoundaryReturnsNullForOffsetAtEnd() {
        // Offset == total length (5) is past the last char.
        val result = SelectableOcrTextView.wordBoundaryFromTokens(
            offset = 5,
            tokens = sampleTokens,
        )
        assertEquals(null, result)
    }

    @Test
    fun wordBoundaryReturnsNullForEmptyTokens() {
        val result = SelectableOcrTextView.wordBoundaryFromTokens(
            offset = 0,
            tokens = emptyList(),
        )
        assertEquals(null, result)
    }

    // --- promptMenuItemId / promptIdForMenuItem tests ---

    private val samplePrompts = listOf(10L to "Explain", 20L to "Grammar", 30L to "Nuance")

    @Test
    fun promptMenuItemIdRoundTripsThroughPromptIdForMenuItem() {
        samplePrompts.forEachIndexed { index, (promptId, _) ->
            val menuItemId = SelectableOcrTextView.promptMenuItemId(index)
            assertEquals(promptId, SelectableOcrTextView.promptIdForMenuItem(menuItemId, samplePrompts))
        }
    }

    @Test
    fun promptIdForMenuItemReturnsNullForOutOfRangeIds() {
        val beyondLast = SelectableOcrTextView.promptMenuItemId(samplePrompts.size)
        assertEquals(null, SelectableOcrTextView.promptIdForMenuItem(beyondLast, samplePrompts))

        val beforeFirst = SelectableOcrTextView.promptMenuItemId(0) - 1
        assertEquals(null, SelectableOcrTextView.promptIdForMenuItem(beforeFirst, samplePrompts))
    }

    @Test
    fun promptIdForMenuItemMustResolveAgainstMenuSnapshotNotMutatedList() {
        // The action-mode menu is built from a snapshot of the prompt list.
        // If the live list grows while the menu is open (e.g. a translation
        // completes), resolving the clicked index against the mutated list
        // would dispatch the wrong prompt — only the snapshot is correct.
        val snapshot = listOf(10L to "Explain", 20L to "Grammar")
        val mutated = listOf(99L to "Compare") + snapshot

        val clickedItemId = SelectableOcrTextView.promptMenuItemId(1)

        assertEquals(20L, SelectableOcrTextView.promptIdForMenuItem(clickedItemId, snapshot))
        // Same item id against the mutated list resolves to a different prompt:
        assertEquals(10L, SelectableOcrTextView.promptIdForMenuItem(clickedItemId, mutated))
    }

    @Test
    fun promptIdForMenuItemReturnsNullForEmptyPrompts() {
        val menuItemId = SelectableOcrTextView.promptMenuItemId(0)
        assertEquals(null, SelectableOcrTextView.promptIdForMenuItem(menuItemId, emptyList()))
    }

    // --- safeSelection tests ---

    @Test
    fun safeSelectionReturnsOrderedBoundsForNormalSelection() {
        assertEquals(2 to 5, SelectableOcrTextView.safeSelection(2, 5, 10))
    }

    @Test
    fun safeSelectionReordersInvertedBounds() {
        assertEquals(2 to 5, SelectableOcrTextView.safeSelection(5, 2, 10))
    }

    @Test
    fun safeSelectionClampsNegativeStartToZero() {
        assertEquals(0 to 3, SelectableOcrTextView.safeSelection(-1, 3, 10))
    }

    @Test
    fun safeSelectionClampsEndOverflowToTextLength() {
        assertEquals(2 to 10, SelectableOcrTextView.safeSelection(2, 15, 10))
    }

    @Test
    fun safeSelectionReturnsNullForEmptySelection() {
        assertEquals(null, SelectableOcrTextView.safeSelection(3, 3, 10))
    }

    @Test
    fun safeSelectionReturnsNullWhenBothBoundsInvalid() {
        assertEquals(null, SelectableOcrTextView.safeSelection(-5, -1, 10))
    }

    @Test
    fun safeSelectionReturnsNullForEmptyText() {
        assertEquals(null, SelectableOcrTextView.safeSelection(0, 4, 0))
    }
}
