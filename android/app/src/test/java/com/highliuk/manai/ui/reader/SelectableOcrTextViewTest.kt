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
