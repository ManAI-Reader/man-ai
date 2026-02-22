package com.highliuk.manai.data.repository

import com.highliuk.manai.data.local.dao.PageOcrResultDao
import com.highliuk.manai.data.local.entity.PageOcrResultEntity
import com.highliuk.manai.domain.ml.TextRegion
import com.highliuk.manai.domain.model.PageRegion
import com.highliuk.manai.domain.repository.OcrCacheRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class OcrCacheRepositoryImpl @Inject constructor(
    private val dao: PageOcrResultDao
) : OcrCacheRepository {

    override fun observeRegions(mangaId: Long, pageIndex: Int): Flow<List<PageRegion>> =
        dao.getByPage(mangaId, pageIndex).map { entities ->
            entities.map { it.toPageRegion() }
        }

    override suspend fun hasDetectionResults(mangaId: Long, pageIndex: Int): Boolean =
        dao.hasDetectionResults(mangaId, pageIndex)

    override suspend fun getRegions(mangaId: Long, pageIndex: Int): List<PageRegion> =
        dao.getByPageOnce(mangaId, pageIndex).map { it.toPageRegion() }

    override suspend fun saveDetectionResults(
        mangaId: Long,
        pageIndex: Int,
        regions: List<TextRegion>,
        bitmapWidth: Int,
        bitmapHeight: Int,
    ) {
        val entities = regions.mapIndexed { index, region ->
            PageOcrResultEntity(
                mangaId = mangaId,
                pageIndex = pageIndex,
                regionIndex = index,
                normX1 = region.x1 / bitmapWidth,
                normY1 = region.y1 / bitmapHeight,
                normX2 = region.x2 / bitmapWidth,
                normY2 = region.y2 / bitmapHeight,
                confidence = region.confidence,
                ocrText = null,
            )
        }
        dao.insertAll(entities)
    }

    override suspend fun saveOcrResult(
        mangaId: Long,
        pageIndex: Int,
        regionIndex: Int,
        text: String,
    ) {
        dao.updateOcrText(mangaId, pageIndex, regionIndex, text)
    }

    private fun PageOcrResultEntity.toPageRegion(): PageRegion = PageRegion(
        regionIndex = regionIndex,
        normX1 = normX1,
        normY1 = normY1,
        normX2 = normX2,
        normY2 = normY2,
        confidence = confidence,
        ocrText = ocrText,
    )
}
