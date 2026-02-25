package com.highliuk.manai.domain.usecase

import android.graphics.Bitmap
import com.highliuk.manai.domain.ml.TextDetector
import com.highliuk.manai.domain.ml.TextRecognizer
import com.highliuk.manai.domain.ml.TextRegion
import com.highliuk.manai.domain.repository.OcrCacheRepository
import javax.inject.Inject
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.ensureActive

class ProcessPageUseCase @Inject constructor(
    private val textDetector: TextDetector,
    private val textRecognizer: TextRecognizer,
    private val ocrCache: OcrCacheRepository,
) {
    suspend fun execute(
        mangaId: Long,
        pageIndex: Int,
        bitmap: Bitmap,
        detectionOnly: Boolean = false,
        priorityRegionIndex: Int? = null,
    ) {
        val hasCachedDetection = ocrCache.hasDetectionResults(mangaId, pageIndex)

        if (!hasCachedDetection) {
            val regions = textDetector.detect(bitmap)
            ocrCache.saveDetectionResults(mangaId, pageIndex, regions, bitmap.width, bitmap.height)
        }

        if (detectionOnly) return

        val regions = ocrCache.getRegions(mangaId, pageIndex)
        val pendingRegions = regions.filter { it.ocrText == null }

        val sorted = if (priorityRegionIndex != null) {
            val priority = pendingRegions.filter { it.regionIndex == priorityRegionIndex }
            val rest = pendingRegions.filter { it.regionIndex != priorityRegionIndex }
            priority + rest
        } else {
            pendingRegions
        }

        for (region in sorted) {
            coroutineContext.ensureActive()
            val textRegion = TextRegion(
                x1 = region.normX1 * bitmap.width,
                y1 = region.normY1 * bitmap.height,
                x2 = region.normX2 * bitmap.width,
                y2 = region.normY2 * bitmap.height,
                confidence = region.confidence,
            )
            val result = textRecognizer.recognize(bitmap, textRegion)
            ocrCache.saveOcrResult(mangaId, pageIndex, region.regionIndex, result.text)
        }
    }
}
