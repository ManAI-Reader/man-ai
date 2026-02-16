package com.highliuk.manai.ui.reader

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReaderGestureStateTest {

    @Test
    fun `top bar is hidden by default`() {
        val state = ReaderGestureState()
        assertFalse(state.isTopBarVisible)
    }

    @Test
    fun `toggleTopBar shows top bar when hidden`() {
        val state = ReaderGestureState()
        state.toggleTopBar()
        assertTrue(state.isTopBarVisible)
    }

    @Test
    fun `toggleTopBar hides top bar when visible`() {
        val state = ReaderGestureState()
        state.toggleTopBar()
        state.toggleTopBar()
        assertFalse(state.isTopBarVisible)
    }

    @Test
    fun `scale starts at 1f`() {
        val state = ReaderGestureState()
        assertEquals(1f, state.scale)
    }

    @Test
    fun `offsets start at 0f`() {
        val state = ReaderGestureState()
        assertEquals(0f, state.offsetX)
        assertEquals(0f, state.offsetY)
    }

    @Test
    fun `isZoomed is false at default scale`() {
        val state = ReaderGestureState()
        assertFalse(state.isZoomed)
    }

    @Test
    fun `onZoom multiplies scale`() {
        val state = ReaderGestureState()
        state.onZoom(1.5f)
        assertEquals(1.5f, state.scale, 0.001f)
    }

    @Test
    fun `onZoom clamps scale at max 3f`() {
        val state = ReaderGestureState()
        state.onZoom(10f)
        assertEquals(3f, state.scale, 0.001f)
    }

    @Test
    fun `onZoom clamps scale at min 1f`() {
        val state = ReaderGestureState()
        state.onZoom(2f)
        state.onZoom(0.1f)
        assertEquals(1f, state.scale, 0.001f)
    }

    @Test
    fun `isZoomed returns true when scale above 1f`() {
        val state = ReaderGestureState()
        state.onZoom(1.5f)
        assertTrue(state.isZoomed)
    }

    @Test
    fun `onPan updates offsets when zoomed`() {
        val state = ReaderGestureState()
        state.onZoom(2f)
        state.onPan(50f, 30f, 1000f, 2000f)
        assertEquals(50f, state.offsetX, 0.001f)
        assertEquals(30f, state.offsetY, 0.001f)
    }

    @Test
    fun `onPan clamps offsets to page bounds`() {
        val state = ReaderGestureState()
        state.onZoom(2f)
        state.onPan(9999f, 9999f, 1000f, 2000f)
        assertEquals(500f, state.offsetX, 0.001f)
        assertEquals(1000f, state.offsetY, 0.001f)
    }

    @Test
    fun `onPan clamps negative offsets`() {
        val state = ReaderGestureState()
        state.onZoom(2f)
        state.onPan(-9999f, -9999f, 1000f, 2000f)
        assertEquals(-500f, state.offsetX, 0.001f)
        assertEquals(-1000f, state.offsetY, 0.001f)
    }

    @Test
    fun `onPan is ignored when not zoomed`() {
        val state = ReaderGestureState()
        state.onPan(100f, 100f, 1000f, 2000f)
        assertEquals(0f, state.offsetX, 0.001f)
        assertEquals(0f, state.offsetY, 0.001f)
    }

    @Test
    fun `onPan accumulates offsets`() {
        val state = ReaderGestureState()
        state.onZoom(2f)
        state.onPan(50f, 30f, 1000f, 2000f)
        state.onPan(50f, 30f, 1000f, 2000f)
        assertEquals(100f, state.offsetX, 0.001f)
        assertEquals(60f, state.offsetY, 0.001f)
    }

    @Test
    fun `resetZoom sets scale to 1f and offsets to 0f`() {
        val state = ReaderGestureState()
        state.onZoom(2.5f)
        state.onPan(100f, 200f, 1000f, 2000f)
        state.resetZoom()
        assertEquals(1f, state.scale, 0.001f)
        assertEquals(0f, state.offsetX, 0.001f)
        assertEquals(0f, state.offsetY, 0.001f)
    }

    @Test
    fun `zoom back to 1f resets offsets`() {
        val state = ReaderGestureState()
        state.onZoom(2f)
        state.onPan(50f, 50f, 1000f, 1000f)
        state.onZoom(0.5f)
        assertEquals(0f, state.offsetX, 0.001f)
        assertEquals(0f, state.offsetY, 0.001f)
    }
}
