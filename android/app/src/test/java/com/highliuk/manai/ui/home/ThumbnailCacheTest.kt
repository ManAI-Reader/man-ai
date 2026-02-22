package com.highliuk.manai.ui.home

import android.graphics.Bitmap
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ThumbnailCacheTest {

    private val cache = ThumbnailCache()

    @Test
    fun getReturnsNullForUncachedManga() {
        assertNull(cache.get(1L))
    }

    @Test
    fun getReturnsCachedBitmap() {
        val bitmap = mockk<Bitmap>()
        cache.put(1L, bitmap)
        assertEquals(bitmap, cache.get(1L))
    }

    @Test
    fun putOverwritesPreviousEntry() {
        val first = mockk<Bitmap>()
        val second = mockk<Bitmap>()
        cache.put(1L, first)
        cache.put(1L, second)
        assertEquals(second, cache.get(1L))
    }
}
