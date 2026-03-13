package com.highliuk.manai.ui.reader

import com.highliuk.manai.domain.model.PageRegion
import org.junit.Assert.assertEquals
import org.junit.Test

class OverlayCoordinateMapperTest {

    @Test
    fun `FillWidth scales region to container width and centers vertically`() {
        // Bitmap 1000x2000, container 500x1000 (same aspect ratio)
        // FillWidth scale = 500/1000 = 0.5
        // Scaled image height = 2000 * 0.5 = 1000 (fills container exactly)
        // offsetY = (1000 - 1000) / 2 = 0
        val region = PageRegion(0, 0.1f, 0.2f, 0.5f, 0.6f, 0.9f, null)
        val rect = OverlayCoordinateMapper.mapRegion(
            region = region,
            bitmapWidth = 1000,
            bitmapHeight = 2000,
            containerWidth = 500f,
            containerHeight = 1000f,
        )

        assertEquals(50f, rect.left, 0.01f)   // 0.1 * 1000 * 0.5
        assertEquals(200f, rect.top, 0.01f)    // 0.2 * 2000 * 0.5 + 0
        assertEquals(250f, rect.right, 0.01f)  // 0.5 * 1000 * 0.5
        assertEquals(600f, rect.bottom, 0.01f) // 0.6 * 2000 * 0.5 + 0
    }

    @Test
    fun `FillWidth centers vertically when container is taller than scaled image`() {
        // Bitmap 1000x1000, container 500x1000
        // FillWidth scale = 500/1000 = 0.5
        // Scaled image height = 1000 * 0.5 = 500
        // offsetY = (1000 - 500) / 2 = 250
        val region = PageRegion(0, 0.0f, 0.0f, 1.0f, 1.0f, 0.9f, null)
        val rect = OverlayCoordinateMapper.mapRegion(
            region = region,
            bitmapWidth = 1000,
            bitmapHeight = 1000,
            containerWidth = 500f,
            containerHeight = 1000f,
        )

        assertEquals(0f, rect.left, 0.01f)
        assertEquals(250f, rect.top, 0.01f)    // 0 + 250 offsetY
        assertEquals(500f, rect.right, 0.01f)
        assertEquals(750f, rect.bottom, 0.01f)  // 500 + 250 offsetY
    }

    @Test
    fun `FillWidth with portrait bitmap in landscape-ish container`() {
        // Bitmap 800x1200, container 400x800
        // FillWidth scale = 400/800 = 0.5
        // Scaled image height = 1200 * 0.5 = 600
        // offsetY = (800 - 600) / 2 = 100
        val region = PageRegion(0, 0.25f, 0.25f, 0.75f, 0.75f, 0.9f, null)
        val rect = OverlayCoordinateMapper.mapRegion(
            region = region,
            bitmapWidth = 800,
            bitmapHeight = 1200,
            containerWidth = 400f,
            containerHeight = 800f,
        )

        assertEquals(100f, rect.left, 0.01f)   // 0.25 * 800 * 0.5
        assertEquals(250f, rect.top, 0.01f)     // 0.25 * 1200 * 0.5 + 100
        assertEquals(300f, rect.right, 0.01f)   // 0.75 * 800 * 0.5
        assertEquals(550f, rect.bottom, 0.01f)  // 0.75 * 1200 * 0.5 + 100
    }

    @Test
    fun `FillHeight scales region to container height and centers horizontally`() {
        // Bitmap 1000x2000 (portrait), container 1920x1080 (landscape)
        // FillHeight scale = 1080/2000 = 0.54
        // Scaled image width = 1000 * 0.54 = 540
        // offsetX = (1920 - 540) / 2 = 690
        val region = PageRegion(0, 0.1f, 0.2f, 0.5f, 0.6f, 0.9f, null)
        val rect = OverlayCoordinateMapper.mapRegion(
            region = region,
            bitmapWidth = 1000,
            bitmapHeight = 2000,
            containerWidth = 1920f,
            containerHeight = 1080f,
        )

        val scale = 1080f / 2000f
        val offsetX = (1920f - 1000f * scale) / 2f
        assertEquals(0.1f * 1000f * scale + offsetX, rect.left, 0.01f)
        assertEquals(0.2f * 2000f * scale, rect.top, 0.01f)
        assertEquals(0.5f * 1000f * scale + offsetX, rect.right, 0.01f)
        assertEquals(0.6f * 2000f * scale, rect.bottom, 0.01f)
    }

    @Test
    fun `FillHeight with landscape image in landscape container`() {
        // Bitmap 1600x900 (landscape), container 1920x1080 (landscape)
        // imageAspect = 1.78, containerAspect = 1.78 → FillHeight
        // FillHeight scale = 1080/900 = 1.2
        // Scaled image width = 1600 * 1.2 = 1920
        // offsetX = (1920 - 1920) / 2 = 0
        val region = PageRegion(0, 0.0f, 0.0f, 1.0f, 1.0f, 0.9f, null)
        val rect = OverlayCoordinateMapper.mapRegion(
            region = region,
            bitmapWidth = 1600,
            bitmapHeight = 900,
            containerWidth = 1920f,
            containerHeight = 1080f,
        )

        assertEquals(0f, rect.left, 0.01f)
        assertEquals(0f, rect.top, 0.01f)
        assertEquals(1920f, rect.right, 0.01f)
        assertEquals(1080f, rect.bottom, 0.01f)
    }
}
