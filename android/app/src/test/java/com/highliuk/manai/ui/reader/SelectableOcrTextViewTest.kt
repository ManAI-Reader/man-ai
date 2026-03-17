package com.highliuk.manai.ui.reader

import org.junit.Assert.assertEquals
import org.junit.Test

class SelectableOcrTextViewTest {

    // --- computeWordSelectionAdjustment tests ---

    @Test
    fun adjustmentReturnsNullWhenNotPending() {
        val result = SelectableOcrTextView.computeWordSelectionAdjustment(
            pending = false,
            touchedOffset = 5,
            nativeSelStart = 4,
            nativeSelEnd = 7,
            textLength = 20,
        )
        assertEquals(null, result)
    }

    @Test
    fun adjustmentReturnsBoundaryWhenPending() {
        // mockWordBoundary(5, 20) = (4, 7), native selected (3, 8) → adjust to (4, 7)
        val result = SelectableOcrTextView.computeWordSelectionAdjustment(
            pending = true,
            touchedOffset = 5,
            nativeSelStart = 3,
            nativeSelEnd = 8,
            textLength = 20,
        )
        assertEquals(4 to 7, result)
    }

    @Test
    fun adjustmentReturnsNullForCursorSelection() {
        val result = SelectableOcrTextView.computeWordSelectionAdjustment(
            pending = true,
            touchedOffset = 5,
            nativeSelStart = 5,
            nativeSelEnd = 5,
            textLength = 20,
        )
        assertEquals(null, result)
    }

    @Test
    fun adjustmentReturnsNullWhenBoundaryMatchesNative() {
        // mockWordBoundary(3, 10) returns (2, 5)
        // If native already selected (2, 5), no adjustment needed
        val result = SelectableOcrTextView.computeWordSelectionAdjustment(
            pending = true,
            touchedOffset = 3,
            nativeSelStart = 2,
            nativeSelEnd = 5,
            textLength = 10,
        )
        assertEquals(null, result)
    }

    // --- mockWordBoundary tests ---

    @Test
    fun mockWordBoundarySelectsThreeChars() {
        val (start, end) = SelectableOcrTextView.mockWordBoundary(
            offset = 3,
            textLength = 10,
        )
        assertEquals(2, start)
        assertEquals(5, end)
    }

    @Test
    fun mockWordBoundaryClampsAtStart() {
        val (start, end) = SelectableOcrTextView.mockWordBoundary(
            offset = 0,
            textLength = 10,
        )
        assertEquals(0, start)
        assertEquals(2, end)
    }

    @Test
    fun mockWordBoundaryClampsAtEnd() {
        val (start, end) = SelectableOcrTextView.mockWordBoundary(
            offset = 9,
            textLength = 10,
        )
        assertEquals(8, start)
        assertEquals(10, end)
    }

    @Test
    fun mockWordBoundaryHandlesSingleChar() {
        val (start, end) = SelectableOcrTextView.mockWordBoundary(
            offset = 0,
            textLength = 1,
        )
        assertEquals(0, start)
        assertEquals(1, end)
    }

    @Test
    fun mockWordBoundaryHandlesTwoCharsAtStart() {
        val (start, end) = SelectableOcrTextView.mockWordBoundary(
            offset = 0,
            textLength = 2,
        )
        assertEquals(0, start)
        assertEquals(2, end)
    }

    @Test
    fun mockWordBoundaryHandlesTwoCharsAtEnd() {
        val (start, end) = SelectableOcrTextView.mockWordBoundary(
            offset = 1,
            textLength = 2,
        )
        assertEquals(0, start)
        assertEquals(2, end)
    }
}
