package com.highliuk.manai.ui.home

import android.graphics.Bitmap
import java.util.concurrent.ConcurrentHashMap

class ThumbnailCache {

    private val cache = ConcurrentHashMap<Long, Bitmap>()

    fun get(mangaId: Long): Bitmap? = cache[mangaId]

    fun put(mangaId: Long, bitmap: Bitmap) {
        cache[mangaId] = bitmap
    }
}
