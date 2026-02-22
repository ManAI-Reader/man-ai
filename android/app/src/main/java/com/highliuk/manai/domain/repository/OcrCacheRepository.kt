package com.highliuk.manai.domain.repository

import com.highliuk.manai.domain.ml.TextRegion
import com.highliuk.manai.domain.model.PageRegion
import kotlinx.coroutines.flow.Flow

interface OcrCacheRepository {
    fun observeRegions(mangaId: Long, pageIndex: Int): Flow<List<PageRegion>>
    suspend fun hasDetectionResults(mangaId: Long, pageIndex: Int): Boolean
    suspend fun getRegions(mangaId: Long, pageIndex: Int): List<PageRegion>
    suspend fun saveDetectionResults(
        mangaId: Long,
        pageIndex: Int,
        regions: List<TextRegion>,
        bitmapWidth: Int,
        bitmapHeight: Int,
    )
    suspend fun saveOcrResult(mangaId: Long, pageIndex: Int, regionIndex: Int, text: String)
}
