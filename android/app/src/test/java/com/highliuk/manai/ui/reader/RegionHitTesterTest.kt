package com.highliuk.manai.ui.reader

import com.highliuk.manai.domain.model.PageRegion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class RegionHitTesterTest {

    // Container: 1080x1920, Bitmap: 1080x1520 (fits width, centered vertically)
    // renderedImageHeight = 1080 * 1520 / 1080 = 1520
    // verticalPadding = (1920 - 1520) / 2 = 200

    @Test
    fun `screenToNormalized at center of unzoomed image returns 0_5, 0_5`() {
        val result = RegionHitTester.screenToNormalized(
            tapX = 540f, tapY = 960f,
            containerWidth = 1080f, containerHeight = 1920f,
            bitmapWidth = 1080, bitmapHeight = 1520,
            scale = 1f, offsetX = 0f, offsetY = 0f,
        )
        assertNotNull(result)
        assertEquals(0.5f, result!!.first, 0.01f)
        assertEquals(0.5f, result.second, 0.01f)
    }

    @Test
    fun `screenToNormalized at top-left of image returns 0, 0`() {
        // Image top-left is at screen (0, 200) due to vertical padding
        val result = RegionHitTester.screenToNormalized(
            tapX = 0f, tapY = 200f,
            containerWidth = 1080f, containerHeight = 1920f,
            bitmapWidth = 1080, bitmapHeight = 1520,
            scale = 1f, offsetX = 0f, offsetY = 0f,
        )
        assertNotNull(result)
        assertEquals(0f, result!!.first, 0.01f)
        assertEquals(0f, result.second, 0.01f)
    }

    @Test
    fun `screenToNormalized outside image bounds returns null`() {
        // Tap in the vertical padding area (y = 100, below top padding of 200)
        val result = RegionHitTester.screenToNormalized(
            tapX = 540f, tapY = 100f,
            containerWidth = 1080f, containerHeight = 1920f,
            bitmapWidth = 1080, bitmapHeight = 1520,
            scale = 1f, offsetX = 0f, offsetY = 0f,
        )
        assertNull(result)
    }

    @Test
    fun `screenToNormalized with 2x zoom and pan`() {
        // Zoomed 2x, panned left by 200px
        val result = RegionHitTester.screenToNormalized(
            tapX = 540f, tapY = 960f,
            containerWidth = 1080f, containerHeight = 1920f,
            bitmapWidth = 1080, bitmapHeight = 1520,
            scale = 2f, offsetX = -200f, offsetY = 0f,
        )
        assertNotNull(result)
        // Inverse: contentX = (540-540)/2 + 540 - (-200)/2 = 0 + 540 + 100 = 640
        // normX = 640/1080 ~ 0.593
        assertEquals(0.593f, result!!.first, 0.01f)
    }

    @Test
    fun `hitTest returns matching region`() {
        val regions = listOf(
            PageRegion(0, 0.1f, 0.1f, 0.3f, 0.3f, 0.9f, "hello"),
            PageRegion(1, 0.5f, 0.5f, 0.7f, 0.7f, 0.8f, "world"),
        )
        // Tap at normalized (0.6, 0.6) - inside region 1
        // Screen coords: x = 0.6*1080 = 648, y = 200 + 0.6*1520 = 1112
        val hit = RegionHitTester.hitTest(
            tapX = 648f, tapY = 1112f,
            regions = regions,
            containerWidth = 1080f, containerHeight = 1920f,
            bitmapWidth = 1080, bitmapHeight = 1520,
            scale = 1f, offsetX = 0f, offsetY = 0f,
        )
        assertNotNull(hit)
        assertEquals(1, hit!!.regionIndex)
    }

    @Test
    fun `hitTest returns null when no region matches`() {
        val regions = listOf(
            PageRegion(0, 0.1f, 0.1f, 0.3f, 0.3f, 0.9f, "hello"),
        )
        // Tap at normalized (0.5, 0.5) - outside region 0
        val hit = RegionHitTester.hitTest(
            tapX = 540f, tapY = 960f,
            regions = regions,
            containerWidth = 1080f, containerHeight = 1920f,
            bitmapWidth = 1080, bitmapHeight = 1520,
            scale = 1f, offsetX = 0f, offsetY = 0f,
        )
        assertNull(hit)
    }

    @Test
    fun `hitTest returns highest confidence when regions overlap`() {
        val regions = listOf(
            PageRegion(0, 0.4f, 0.4f, 0.6f, 0.6f, 0.7f, "low"),
            PageRegion(1, 0.4f, 0.4f, 0.6f, 0.6f, 0.95f, "high"),
        )
        val hit = RegionHitTester.hitTest(
            tapX = 540f, tapY = 960f,
            regions = regions,
            containerWidth = 1080f, containerHeight = 1920f,
            bitmapWidth = 1080, bitmapHeight = 1520,
            scale = 1f, offsetX = 0f, offsetY = 0f,
        )
        assertNotNull(hit)
        assertEquals(1, hit!!.regionIndex)
        assertEquals(0.95f, hit.confidence, 0.01f)
    }
}
