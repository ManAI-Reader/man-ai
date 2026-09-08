package com.highliuk.manai.ui.chat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ListMarkerTest {

    @Test
    fun bulletItemsUseTheBulletGlyph() {
        assertEquals("•", listItemMarker(ordered = false, index = 7))
    }

    @Test
    fun orderedItemsUseTheirIndexWithADot() {
        assertEquals("3.", listItemMarker(ordered = true, index = 3))
        assertEquals("12.", listItemMarker(ordered = true, index = 12))
    }

    @Test
    fun markerMarginCeilsWidthPlusGapSoTheMarkerNeverClips() {
        assertEquals(23, listMarkerMarginPx(markerWidthPx = 10.4f, gapPx = 12f))
        assertEquals(22, listMarkerMarginPx(markerWidthPx = 10f, gapPx = 12f))
    }

    @Test
    fun markerIsDrawnOnlyOnTheLineWhereTheSpanStarts() {
        assertTrue(shouldDrawMarkerOnLine(spanStart = 0, lineStart = 0))
        // Wrapped continuation lines of the same paragraph keep the indent
        // but must not repeat the marker.
        assertFalse(shouldDrawMarkerOnLine(spanStart = 0, lineStart = 15))
    }
}
