package com.highliuk.manai.ui.reader

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReaderGestureStateTest {

    @Test
    fun `bars are hidden by default`() {
        val state = ReaderGestureState()
        assertFalse(state.areBarsVisible)
    }

    @Test
    fun `toggleBars shows bars when hidden`() {
        val state = ReaderGestureState()
        state.toggleBars()
        assertTrue(state.areBarsVisible)
    }

    @Test
    fun `toggleBars hides bars when visible`() {
        val state = ReaderGestureState()
        state.toggleBars()
        state.toggleBars()
        assertFalse(state.areBarsVisible)
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
    fun `onPan clamps Y offset to image edge when image is shorter than container`() {
        val state = ReaderGestureState()
        state.setContentSize(1000f, 1500f)
        state.onZoom(2f)
        // container 1000x2000, rendered image 1000x1500 (FillWidth)
        // maxOffsetY = max(0, 2 * 1500/2 - 2000/2) = 500
        state.onPan(0f, 9999f, 1000f, 2000f)
        assertEquals(500f, state.offsetY, 0.001f)
    }

    @Test
    fun `onPan clamps negative Y offset to image edge when image is shorter than container`() {
        val state = ReaderGestureState()
        state.setContentSize(1000f, 1500f)
        state.onZoom(2f)
        state.onPan(0f, -9999f, 1000f, 2000f)
        assertEquals(-500f, state.offsetY, 0.001f)
    }

    @Test
    fun `onPan X offset unchanged when image fills width`() {
        val state = ReaderGestureState()
        state.setContentSize(1000f, 1500f)
        state.onZoom(2f)
        // X: FillWidth → renderedWidth = containerWidth → same formula
        // maxOffsetX = 1000 * (2-1) / 2 = 500
        state.onPan(9999f, 0f, 1000f, 2000f)
        assertEquals(500f, state.offsetX, 0.001f)
    }

    @Test
    fun `onPan without content size uses container bounds`() {
        val state = ReaderGestureState()
        state.onZoom(2f)
        state.onPan(9999f, 9999f, 1000f, 2000f)
        assertEquals(500f, state.offsetX, 0.001f)
        assertEquals(1000f, state.offsetY, 0.001f)
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

    @Test
    fun `onDoubleTap at 1x returns target scale 2f`() {
        val state = ReaderGestureState()
        val target = state.onDoubleTap(500f, 1000f, 1000f, 2000f)
        assertEquals(2f, target.scale, 0.001f)
    }

    @Test
    fun `onDoubleTap when zoomed returns target scale 1f`() {
        val state = ReaderGestureState()
        state.onZoom(2f)
        val target = state.onDoubleTap(500f, 1000f, 1000f, 2000f)
        assertEquals(1f, target.scale, 0.001f)
    }

    @Test
    fun `onDoubleTap when zoomed returns zero offsets`() {
        val state = ReaderGestureState()
        state.onZoom(2f)
        val target = state.onDoubleTap(500f, 1000f, 1000f, 2000f)
        assertEquals(0f, target.offsetX, 0.001f)
        assertEquals(0f, target.offsetY, 0.001f)
    }

    @Test
    fun `onDoubleTap at 1x centers offset on tap point`() {
        val state = ReaderGestureState()
        val target = state.onDoubleTap(250f, 500f, 1000f, 2000f)
        assertEquals(250f, target.offsetX, 0.001f)
        assertEquals(500f, target.offsetY, 0.001f)
    }

    @Test
    fun `onDoubleTap at edge clamps offset to bounds`() {
        val state = ReaderGestureState()
        val target = state.onDoubleTap(0f, 0f, 1000f, 2000f)
        assertEquals(500f, target.offsetX, 0.001f)
        assertEquals(1000f, target.offsetY, 0.001f)
    }

    @Test
    fun `onDoubleTap at 1_5x from pinch returns target 1f`() {
        val state = ReaderGestureState()
        state.onZoom(1.5f)
        val target = state.onDoubleTap(500f, 1000f, 1000f, 2000f)
        assertEquals(1f, target.scale, 0.001f)
    }

    @Test
    fun `applyZoomTarget updates scale and offsets`() {
        val state = ReaderGestureState()
        val target = ZoomTarget(2f, 100f, 200f)
        state.applyZoomTarget(target)
        assertEquals(2f, state.scale, 0.001f)
        assertEquals(100f, state.offsetX, 0.001f)
        assertEquals(200f, state.offsetY, 0.001f)
    }
}
