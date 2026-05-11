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
}
