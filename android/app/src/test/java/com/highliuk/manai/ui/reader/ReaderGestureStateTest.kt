package com.highliuk.manai.ui.reader

import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.geometry.Offset
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
        state.onZoom(1.5f, Offset(500f, 1000f), 1000f, 2000f)
        assertEquals(1.5f, state.scale, 0.001f)
    }

    @Test
    fun `onZoom clamps scale at max 5f`() {
        val state = ReaderGestureState()
        state.onZoom(10f, Offset(500f, 1000f), 1000f, 2000f)
        assertEquals(5f, state.scale, 0.001f)
    }

    @Test
    fun `onZoom clamps scale at min 1f`() {
        val state = ReaderGestureState()
        state.onZoom(2f, Offset(500f, 1000f), 1000f, 2000f)
        state.onZoom(0.1f, Offset(500f, 1000f), 1000f, 2000f)
        assertEquals(1f, state.scale, 0.001f)
    }

    @Test
    fun `isZoomed returns true when scale above 1f`() {
        val state = ReaderGestureState()
        state.onZoom(1.5f, Offset(500f, 1000f), 1000f, 2000f)
        assertTrue(state.isZoomed)
    }

    @Test
    fun `onPan updates offsets when zoomed`() {
        val state = ReaderGestureState()
        state.onZoom(2f, Offset(500f, 1000f), 1000f, 2000f)
        state.onPan(50f, 30f, 1000f, 2000f)
        assertEquals(50f, state.offsetX, 0.001f)
        assertEquals(30f, state.offsetY, 0.001f)
    }

    @Test
    fun `onPan clamps offsets to page bounds`() {
        val state = ReaderGestureState()
        state.onZoom(2f, Offset(500f, 1000f), 1000f, 2000f)
        state.onPan(9999f, 9999f, 1000f, 2000f)
        assertEquals(500f, state.offsetX, 0.001f)
        assertEquals(1000f, state.offsetY, 0.001f)
    }

    @Test
    fun `onPan clamps negative offsets`() {
        val state = ReaderGestureState()
        state.onZoom(2f, Offset(500f, 1000f), 1000f, 2000f)
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
        state.onZoom(2f, Offset(500f, 1000f), 1000f, 2000f)
        state.onPan(50f, 30f, 1000f, 2000f)
        state.onPan(50f, 30f, 1000f, 2000f)
        assertEquals(100f, state.offsetX, 0.001f)
        assertEquals(60f, state.offsetY, 0.001f)
    }

    @Test
    fun `resetZoom sets scale to 1f and offsets to 0f`() {
        val state = ReaderGestureState()
        state.onZoom(2.5f, Offset(500f, 1000f), 1000f, 2000f)
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
        state.onZoom(2f, Offset(500f, 1000f), 1000f, 2000f)
        // container 1000x2000, rendered image 1000x1500 (FillWidth)
        // maxOffsetY = max(0, 2 * 1500/2 - 2000/2) = 500
        state.onPan(0f, 9999f, 1000f, 2000f)
        assertEquals(500f, state.offsetY, 0.001f)
    }

    @Test
    fun `onPan clamps negative Y offset to image edge when image is shorter than container`() {
        val state = ReaderGestureState()
        state.setContentSize(1000f, 1500f)
        state.onZoom(2f, Offset(500f, 1000f), 1000f, 2000f)
        state.onPan(0f, -9999f, 1000f, 2000f)
        assertEquals(-500f, state.offsetY, 0.001f)
    }

    @Test
    fun `onPan X offset unchanged when image fills width`() {
        val state = ReaderGestureState()
        state.setContentSize(1000f, 1500f)
        state.onZoom(2f, Offset(500f, 1000f), 1000f, 2000f)
        // X: FillWidth -> renderedWidth = containerWidth -> same formula
        // maxOffsetX = 1000 * (2-1) / 2 = 500
        state.onPan(9999f, 0f, 1000f, 2000f)
        assertEquals(500f, state.offsetX, 0.001f)
    }

    @Test
    fun `onPan without content size uses container bounds`() {
        val state = ReaderGestureState()
        state.onZoom(2f, Offset(500f, 1000f), 1000f, 2000f)
        state.onPan(9999f, 9999f, 1000f, 2000f)
        assertEquals(500f, state.offsetX, 0.001f)
        assertEquals(1000f, state.offsetY, 0.001f)
    }

    @Test
    fun `zoom back to 1f resets offsets`() {
        val state = ReaderGestureState()
        state.onZoom(2f, Offset(500f, 1000f), 1000f, 2000f)
        state.onPan(50f, 50f, 1000f, 1000f)
        state.onZoom(0.5f, Offset(500f, 1000f), 1000f, 2000f)
        assertEquals(0f, state.offsetX, 0.001f)
        assertEquals(0f, state.offsetY, 0.001f)
    }

    @Test
    fun `onZoom with centroid at top-left shifts offset toward top-left`() {
        val state = ReaderGestureState()
        // Container 1000x2000, centroid at (0, 0) = top-left corner
        // centroidRelX = 0 - 500 = -500, centroidRelY = 0 - 1000 = -1000
        // oldScale=1, newScale=2, ratio=2
        // offsetX = -500 - (-500 - 0) * 2 = -500 + 1000 = 500 -> clamp to maxX=500 -> 500
        // offsetY = -1000 - (-1000 - 0) * 2 = -1000 + 2000 = 1000 -> clamp to maxY=1000 -> 1000
        state.onZoom(2f, Offset(0f, 0f), 1000f, 2000f)
        assertEquals(2f, state.scale, 0.001f)
        assertEquals(500f, state.offsetX, 0.001f)
        assertEquals(1000f, state.offsetY, 0.001f)
    }

    @Test
    fun `onZoom with centroid at bottom-right shifts offset toward bottom-right`() {
        val state = ReaderGestureState()
        // centroidRelX = 1000-500=500, centroidRelY = 2000-1000=1000
        // offsetX = 500 - (500-0)*2 = 500-1000 = -500 -> clamp to [-500,500] -> -500
        // offsetY = 1000 - (1000-0)*2 = 1000-2000 = -1000 -> clamp to [-1000,1000] -> -1000
        state.onZoom(2f, Offset(1000f, 2000f), 1000f, 2000f)
        assertEquals(-500f, state.offsetX, 0.001f)
        assertEquals(-1000f, state.offsetY, 0.001f)
    }

    @Test
    fun `onZoom with centroid at center produces no offset`() {
        val state = ReaderGestureState()
        state.onZoom(2f, Offset(500f, 1000f), 1000f, 2000f)
        assertEquals(0f, state.offsetX, 0.001f)
        assertEquals(0f, state.offsetY, 0.001f)
    }

    @Test
    fun `onZoom clamps offsets during de-zoom`() {
        val state = ReaderGestureState()
        // Zoom in to 3x with offset at max bounds
        state.onZoom(3f, Offset(500f, 1000f), 1000f, 2000f)
        state.onPan(9999f, 9999f, 1000f, 2000f)
        // At 3x: maxX=1000, maxY=2000
        assertEquals(1000f, state.offsetX, 0.001f)
        assertEquals(2000f, state.offsetY, 0.001f)
        // De-zoom to 2x from center — offsets must be clamped to 2x bounds
        // At 2x: maxX=500, maxY=1000
        state.onZoom(2f / 3f, Offset(500f, 1000f), 1000f, 2000f)
        assertEquals(2f, state.scale, 0.01f)
        assertTrue("offsetX should be <= 500 after de-zoom", state.offsetX <= 500f + 0.01f)
        assertTrue("offsetY should be <= 1000 after de-zoom", state.offsetY <= 1000f + 0.01f)
    }

    @Test
    fun `onZoom de-zoom to 1x resets offsets to zero`() {
        val state = ReaderGestureState()
        state.onZoom(3f, Offset(200f, 400f), 1000f, 2000f)
        // Now de-zoom back to 1x
        state.onZoom(1f / 3f, Offset(200f, 400f), 1000f, 2000f)
        assertEquals(1f, state.scale, 0.01f)
        assertEquals(0f, state.offsetX, 0.001f)
        assertEquals(0f, state.offsetY, 0.001f)
    }

    @Test
    fun `onZoom then de-zoom round-trip from center preserves zero offset`() {
        val state = ReaderGestureState()
        val center = Offset(500f, 1000f)
        state.onZoom(2f, center, 1000f, 2000f)
        assertEquals(0f, state.offsetX, 0.001f)
        state.onZoom(0.5f, center, 1000f, 2000f)
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
        state.onZoom(2f, Offset(500f, 1000f), 1000f, 2000f)
        val target = state.onDoubleTap(500f, 1000f, 1000f, 2000f)
        assertEquals(1f, target.scale, 0.001f)
    }

    @Test
    fun `onDoubleTap when zoomed returns zero offsets`() {
        val state = ReaderGestureState()
        state.onZoom(2f, Offset(500f, 1000f), 1000f, 2000f)
        val target = state.onDoubleTap(500f, 1000f, 1000f, 2000f)
        assertEquals(0f, target.offsetX, 0.001f)
        assertEquals(0f, target.offsetY, 0.001f)
    }

    @Test
    fun `onDoubleTap at 1x centers offset on tap point`() {
        val state = ReaderGestureState()
        state.setContentSize(1000f, 1400f)
        val target = state.onDoubleTap(250f, 500f, 1000f, 2000f)
        assertEquals(250f, target.offsetX, 0.001f)
        assertEquals(500f, target.offsetY, 0.001f)
    }

    @Test
    fun `onDoubleTap at edge clamps offset to bounds`() {
        val state = ReaderGestureState()
        state.setContentSize(1000f, 1400f)
        val target = state.onDoubleTap(0f, 0f, 1000f, 2000f)
        assertEquals(500f, target.offsetX, 0.001f)
        assertEquals(1000f, target.offsetY, 0.001f)
    }

    @Test
    fun `onDoubleTap at 1_5x from pinch returns target 1f`() {
        val state = ReaderGestureState()
        state.onZoom(1.5f, Offset(500f, 1000f), 1000f, 2000f)
        val target = state.onDoubleTap(500f, 1000f, 1000f, 2000f)
        assertEquals(1f, target.scale, 0.001f)
    }

    @Test
    fun `contentWidth and contentHeight are accessible after setContentSize`() {
        val state = ReaderGestureState()
        state.setContentSize(640f, 480f)

        assertEquals(640f, state.contentWidth, 0.01f)
        assertEquals(480f, state.contentHeight, 0.01f)
    }

    @Test
    fun `onPanX updates only offsetX when zoomed`() {
        val state = ReaderGestureState()
        state.onZoom(2f, Offset(500f, 1000f), 1000f, 2000f)

        state.onPanX(50f, 400f)

        assertTrue(state.offsetX != 0f)
        assertEquals(0f, state.offsetY)
    }

    @Test
    fun `onPanX does nothing when not zoomed`() {
        val state = ReaderGestureState()

        state.onPanX(50f, 400f)

        assertEquals(0f, state.offsetX)
    }

    @Test
    fun `onPanX clamps to max offset`() {
        val state = ReaderGestureState()
        state.onZoom(2f, Offset(500f, 1000f), 1000f, 2000f) // maxOffsetX = 400 * (2-1) / 2 = 200

        state.onPanX(999f, 400f)

        assertEquals(200f, state.offsetX)
    }

    @Test
    fun `onDoubleTapWebtoon returns target with zero offsetY`() {
        val state = ReaderGestureState()

        val target = state.onDoubleTapWebtoon(tapX = 100f, containerWidth = 400f)

        assertEquals(2f, target.scale)
        assertEquals(0f, target.offsetY)
    }

    @Test
    fun `onDoubleTapWebtoon when zoomed returns reset target`() {
        val state = ReaderGestureState()
        state.onZoom(2f, Offset(500f, 1000f), 1000f, 2000f)

        val target = state.onDoubleTapWebtoon(tapX = 100f, containerWidth = 400f)

        assertEquals(1f, target.scale)
        assertEquals(0f, target.offsetX)
        assertEquals(0f, target.offsetY)
    }

    @Test
    fun `onDoubleTapWebtoon centers X on tap position`() {
        val state = ReaderGestureState()

        val target = state.onDoubleTapWebtoon(tapX = 100f, containerWidth = 400f)

        // centerX=200, targetOffsetX = (200-100) = 100, maxOffsetX = 400*(2-1)/2 = 200
        assertEquals(100f, target.offsetX)
    }

    @Test
    fun `onDoubleTapWebtoon centers Y on tap position`() {
        val state = ReaderGestureState()

        val target = state.onDoubleTapWebtoon(
            tapX = 100f,
            tapY = 200f,
            containerWidth = 400f,
            containerHeight = 800f
        )

        // centerY=400, targetOffsetY = (400-200) = 200, maxOffsetY = 800*(2-1)/2 = 400
        assertEquals(200f, target.offsetY)
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

    @Test
    fun `onPan clamps X offset to image edge when image is narrower than container`() {
        val state = ReaderGestureState()
        // Portrait image in landscape container (fit-to-height)
        // Image 1000x1500, container 1920x1080
        // Rendered: height fills 1080, width = 1080 * 1000/1500 = 720
        // At scale 3: rendered width = 2160, maxOffsetX = max(0, 2160/2 - 1920/2) = 120
        state.setContentSize(1000f, 1500f)
        state.onZoom(3f, Offset(960f, 540f), 1920f, 1080f)
        state.onPan(9999f, 0f, 1920f, 1080f)
        assertEquals(120f, state.offsetX, 1f)
    }

    @Test
    fun `onPan locks X offset to 0 when fit-to-height image is narrower than container at current zoom`() {
        val state = ReaderGestureState()
        // Image 1000x1500, container 1920x1080
        // Rendered at scale 2: width = 2*720 = 1440 < 1920 -> maxOffsetX = 0
        state.setContentSize(1000f, 1500f)
        state.onZoom(2f, Offset(960f, 540f), 1920f, 1080f)
        state.onPan(9999f, 0f, 1920f, 1080f)
        assertEquals(0f, state.offsetX, 1f)
    }

    @Test
    fun `onPan clamps Y offset when fit-to-height image fills container height`() {
        val state = ReaderGestureState()
        // Image 1000x1500, container 1920x1080
        // Fit-to-height: rendered height = 1080
        // At scale 2: maxOffsetY = max(0, 2*1080/2 - 1080/2) = 540
        state.setContentSize(1000f, 1500f)
        state.onZoom(2f, Offset(960f, 540f), 1920f, 1080f)
        state.onPan(0f, 9999f, 1920f, 1080f)
        assertEquals(540f, state.offsetY, 1f)
    }

    @Test
    fun `onDoubleTap at 1x in landscape returns target scale 3f`() {
        val state = ReaderGestureState()
        state.setContentSize(1000f, 1500f)
        val target = state.onDoubleTap(960f, 540f, 1920f, 1080f)
        assertEquals(3f, target.scale, 0.001f)
    }

    @Test
    fun `onDoubleTap at 1x in landscape centers offset on tap point`() {
        val state = ReaderGestureState()
        state.setContentSize(1000f, 1500f)
        val target = state.onDoubleTap(480f, 270f, 1920f, 1080f)
        assertEquals(960f, target.offsetX, 0.001f)
        assertEquals(540f, target.offsetY, 0.001f)
    }

    @Test
    fun `onDoubleTap at edge in landscape clamps offset to bounds`() {
        val state = ReaderGestureState()
        state.setContentSize(1000f, 1500f)
        val target = state.onDoubleTap(0f, 0f, 1920f, 1080f)
        assertEquals(1920f, target.offsetX, 0.001f)
        assertEquals(1080f, target.offsetY, 0.001f)
    }

    @Test
    fun `contentWidth is reactive Compose state so orientation changes trigger recomposition`() {
        val state = ReaderGestureState()
        state.setContentSize(1000f, 1500f)
        var readTracked = false

        val snapshot = Snapshot.takeMutableSnapshot(
            readObserver = { readTracked = true }
        )
        snapshot.enter {
            state.contentWidth
        }
        snapshot.dispose()

        assertTrue("contentWidth should be tracked by snapshot system", readTracked)
    }

    @Test
    fun `contentHeight is reactive Compose state so content scale updates on bitmap load`() {
        val state = ReaderGestureState()
        state.setContentSize(1000f, 1500f)
        var readTracked = false

        val snapshot = Snapshot.takeMutableSnapshot(
            readObserver = { readTracked = true }
        )
        snapshot.enter {
            state.contentHeight
        }
        snapshot.dispose()

        assertTrue("contentHeight should be tracked by snapshot system", readTracked)
    }

    // --- Ratchet clamp tests for onPan ---

    @Test
    fun `onPan does not snap offset when beyond normal bounds due to zoom`() {
        val state = ReaderGestureState()
        state.setContentSize(2000f, 2500f)
        // container 2000x3000, FillWidth. renderedHeight=2500, at 2x: maxY=(2*2500/2-3000/2)=1000
        state.applyZoomTarget(ZoomTarget(2f, 0f, 1200f))
        // Pan with zero delta — offset must NOT snap to 1000
        state.onPan(0f, 0f, 2000f, 3000f)
        assertEquals(1200f, state.offsetY, 0.001f)
    }

    @Test
    fun `onPan blocks pan away from image when offset beyond normal bounds`() {
        val state = ReaderGestureState()
        state.setContentSize(2000f, 2500f)
        state.applyZoomTarget(ZoomTarget(2f, 0f, 1200f))
        state.onPan(0f, 50f, 2000f, 3000f)
        assertEquals(1200f, state.offsetY, 0.001f)
    }

    @Test
    fun `onPan allows pan toward image when offset beyond normal bounds`() {
        val state = ReaderGestureState()
        state.setContentSize(2000f, 2500f)
        state.applyZoomTarget(ZoomTarget(2f, 0f, 1200f))
        state.onPan(0f, -50f, 2000f, 3000f)
        assertEquals(1150f, state.offsetY, 0.001f)
    }

    @Test
    fun `onPan ratchets - cannot reverse progress toward image`() {
        val state = ReaderGestureState()
        state.setContentSize(2000f, 2500f)
        state.applyZoomTarget(ZoomTarget(2f, 0f, 1200f))
        state.onPan(0f, -50f, 2000f, 3000f)
        assertEquals(1150f, state.offsetY, 0.001f)
        state.onPan(0f, 100f, 2000f, 3000f)
        assertEquals(1150f, state.offsetY, 0.001f)
    }

    @Test
    fun `onPan resumes normal behavior once offset within normal bounds`() {
        val state = ReaderGestureState()
        state.setContentSize(2000f, 2500f)
        state.applyZoomTarget(ZoomTarget(2f, 0f, 1200f))
        state.onPan(0f, -300f, 2000f, 3000f)
        assertEquals(900f, state.offsetY, 0.001f)
        state.onPan(0f, 200f, 2000f, 3000f)
        assertEquals(1000f, state.offsetY, 0.001f)
    }

    @Test
    fun `onPan ratchet works for negative Y offset beyond bounds`() {
        val state = ReaderGestureState()
        state.setContentSize(2000f, 2500f)
        state.applyZoomTarget(ZoomTarget(2f, 0f, -1200f))
        state.onPan(0f, -50f, 2000f, 3000f)
        assertEquals(-1200f, state.offsetY, 0.001f)
        state.onPan(0f, 50f, 2000f, 3000f)
        assertEquals(-1150f, state.offsetY, 0.001f)
    }

    @Test
    fun `onPan ratchet works for X axis beyond bounds`() {
        val state = ReaderGestureState()
        state.setContentSize(1000f, 1500f)
        // landscape container: FillHeight. renderedWidth = 1080*1000/1500 = 720
        // At 3x: maxX = max(0, 3*720/2 - 1920/2) = 120
        state.applyZoomTarget(ZoomTarget(3f, 300f, 0f))
        state.onPan(50f, 0f, 1920f, 1080f)
        assertEquals(300f, state.offsetX, 1f)
        state.onPan(-100f, 0f, 1920f, 1080f)
        assertEquals(200f, state.offsetX, 1f)
    }

    // --- Ratchet clamp tests for onPanX ---

    @Test
    fun `onPanX does not snap offset when beyond normal bounds`() {
        val state = ReaderGestureState()
        // At 2x, containerWidth=400: maxOffsetX = 400*(2-1)/2 = 200
        state.applyZoomTarget(ZoomTarget(2f, 350f, 0f))
        state.onPanX(0f, 400f)
        assertEquals(350f, state.offsetX, 0.001f)
    }

    @Test
    fun `onPanX blocks pan away from image when beyond bounds`() {
        val state = ReaderGestureState()
        state.applyZoomTarget(ZoomTarget(2f, 350f, 0f))
        state.onPanX(50f, 400f)
        assertEquals(350f, state.offsetX, 0.001f)
    }

    @Test
    fun `onPanX allows pan toward image when beyond bounds`() {
        val state = ReaderGestureState()
        state.applyZoomTarget(ZoomTarget(2f, 350f, 0f))
        state.onPanX(-50f, 400f)
        assertEquals(300f, state.offsetX, 0.001f)
    }

    @Test
    fun `onPanX ratchet works for negative offset beyond bounds`() {
        val state = ReaderGestureState()
        state.applyZoomTarget(ZoomTarget(2f, -350f, 0f))
        state.onPanX(-50f, 400f)
        assertEquals(-350f, state.offsetX, 0.001f)
        state.onPanX(50f, 400f)
        assertEquals(-300f, state.offsetX, 0.001f)
    }
}
