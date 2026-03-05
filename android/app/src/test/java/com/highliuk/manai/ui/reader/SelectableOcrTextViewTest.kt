package com.highliuk.manai.ui.reader

import org.junit.Assert.assertEquals
import org.junit.Test

class SelectableOcrTextViewTest {

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
