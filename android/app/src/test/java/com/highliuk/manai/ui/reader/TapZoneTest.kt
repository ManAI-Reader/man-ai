package com.highliuk.manai.ui.reader

import org.junit.Assert.assertEquals
import org.junit.Test

class TapZoneTest {

    @Test
    fun `tap at 0 percent is LEFT`() {
        assertEquals(TapZone.LEFT, classifyTapZone(tapX = 0f, containerWidth = 900f))
    }

    @Test
    fun `tap at 32 percent is LEFT`() {
        assertEquals(TapZone.LEFT, classifyTapZone(tapX = 288f, containerWidth = 900f))
    }

    @Test
    fun `tap at 34 percent is CENTER`() {
        assertEquals(TapZone.CENTER, classifyTapZone(tapX = 306f, containerWidth = 900f))
    }

    @Test
    fun `tap at 50 percent is CENTER`() {
        assertEquals(TapZone.CENTER, classifyTapZone(tapX = 450f, containerWidth = 900f))
    }

    @Test
    fun `tap at 65 percent is CENTER`() {
        assertEquals(TapZone.CENTER, classifyTapZone(tapX = 585f, containerWidth = 900f))
    }

    @Test
    fun `tap at 67 percent is RIGHT`() {
        assertEquals(TapZone.RIGHT, classifyTapZone(tapX = 603f, containerWidth = 900f))
    }

    @Test
    fun `tap at 100 percent is RIGHT`() {
        assertEquals(TapZone.RIGHT, classifyTapZone(tapX = 900f, containerWidth = 900f))
    }

    @Test
    fun `tap exactly at one third boundary is LEFT`() {
        assertEquals(TapZone.LEFT, classifyTapZone(tapX = 300f, containerWidth = 900f))
    }

    @Test
    fun `tap exactly at two thirds boundary is RIGHT`() {
        assertEquals(TapZone.RIGHT, classifyTapZone(tapX = 600f, containerWidth = 900f))
    }
}
