package com.highliuk.manai.domain.usecase

import android.graphics.Bitmap
import com.highliuk.manai.domain.debug.DebugMlEvent
import com.highliuk.manai.domain.debug.DebugMlEventHolder
import com.highliuk.manai.domain.debug.PipelineDebugStateHolder
import com.highliuk.manai.domain.ml.TextDetector
import com.highliuk.manai.domain.ml.TextRecognizer
import com.highliuk.manai.domain.ml.TextRegion
import com.highliuk.manai.domain.model.BalloonPipelineStatus
import com.highliuk.manai.domain.model.PipelineStatus
import com.highliuk.manai.domain.repository.OcrCacheRepository
import javax.inject.Inject
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.ensureActive

class ProcessPageUseCase @Inject constructor(
    private val textDetector: TextDetector,
    private val textRecognizer: TextRecognizer,
    private val ocrCache: OcrCacheRepository,
    private val debugStateHolder: PipelineDebugStateHolder? = null,
    private val debugEventHolder: DebugMlEventHolder? = null,
) {
    suspend fun execute(
        mangaId: Long,
        pageIndex: Int,
        bitmap: Bitmap,
        detectionOnly: Boolean = false,
        priorityRegionIndex: Int? = null,
    ) {
        runDetection(mangaId, pageIndex, bitmap)
        if (detectionOnly) return
        runOcr(mangaId, pageIndex, bitmap, priorityRegionIndex)
    }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun runDetection(mangaId: Long, pageIndex: Int, bitmap: Bitmap) {
        val hasCachedDetection = ocrCache.hasDetectionResults(mangaId, pageIndex)
        if (!hasCachedDetection) {
            debugStateHolder?.setPageStatus(pageIndex, PipelineStatus.Processing)
            try {
                val regions = textDetector.detect(bitmap)
                ocrCache.saveDetectionResults(mangaId, pageIndex, regions, bitmap.width, bitmap.height)
                debugStateHolder?.setPageStatus(pageIndex, PipelineStatus.Done)
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                debugStateHolder?.setPageStatus(pageIndex, PipelineStatus.Error(e.message ?: "Unknown"))
                debugEventHolder?.emit(
                    DebugMlEvent.PipelineError("Detection failed on page $pageIndex: ${e.message}")
                )
                throw e
            }
        } else {
            val current = debugStateHolder?.states?.value?.get(pageIndex)?.pageStatus
            if (current !is PipelineStatus.Done) {
                debugStateHolder?.setPageStatus(pageIndex, PipelineStatus.CacheHit)
            }
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun runOcr(
        mangaId: Long,
        pageIndex: Int,
        bitmap: Bitmap,
        priorityRegionIndex: Int?,
    ) {
        val regions = ocrCache.getRegions(mangaId, pageIndex)
        regions.filter { it.ocrText != null }.forEach { region ->
            val currentBalloon = debugStateHolder?.states?.value
                ?.get(pageIndex)?.balloonStatuses?.get(region.regionIndex)
            if (currentBalloon !is BalloonPipelineStatus.OcrDone) {
                debugStateHolder?.setBalloonStatus(
                    pageIndex, region.regionIndex, BalloonPipelineStatus.OcrCacheHit
                )
            }
        }

        val sorted = sortRegions(regions.filter { it.ocrText == null }, priorityRegionIndex)
        sorted.forEachIndexed { index, region ->
            debugStateHolder?.setBalloonStatus(
                pageIndex, region.regionIndex, BalloonPipelineStatus.OcrQueued(index + 1)
            )
        }

        for ((index, region) in sorted.withIndex()) {
            coroutineContext.ensureActive()
            debugStateHolder?.setBalloonStatus(
                pageIndex, region.regionIndex, BalloonPipelineStatus.OcrProcessing
            )
            updateRemainingQueue(pageIndex, sorted.drop(index + 1))
            recognizeRegion(mangaId, pageIndex, bitmap, region)
        }
    }

    private fun sortRegions(
        pending: List<com.highliuk.manai.domain.model.PageRegion>,
        priorityRegionIndex: Int?,
    ) = if (priorityRegionIndex != null) {
        pending.filter { it.regionIndex == priorityRegionIndex } +
            pending.filter { it.regionIndex != priorityRegionIndex }
    } else {
        pending
    }

    private fun updateRemainingQueue(
        pageIndex: Int,
        remaining: List<com.highliuk.manai.domain.model.PageRegion>,
    ) {
        remaining.forEachIndexed { idx, r ->
            debugStateHolder?.setBalloonStatus(
                pageIndex, r.regionIndex, BalloonPipelineStatus.OcrQueued(idx + 1)
            )
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun recognizeRegion(
        mangaId: Long,
        pageIndex: Int,
        bitmap: Bitmap,
        region: com.highliuk.manai.domain.model.PageRegion,
    ) {
        try {
            val textRegion = TextRegion(
                x1 = region.normX1 * bitmap.width,
                y1 = region.normY1 * bitmap.height,
                x2 = region.normX2 * bitmap.width,
                y2 = region.normY2 * bitmap.height,
                confidence = region.confidence,
            )
            val result = textRecognizer.recognize(bitmap, textRegion)
            ocrCache.saveOcrResult(mangaId, pageIndex, region.regionIndex, result.text)
            debugStateHolder?.setBalloonStatus(
                pageIndex, region.regionIndex, BalloonPipelineStatus.OcrDone
            )
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            debugStateHolder?.setBalloonStatus(
                pageIndex, region.regionIndex,
                BalloonPipelineStatus.OcrError(e.message ?: "Unknown error")
            )
            debugEventHolder?.emit(
                DebugMlEvent.PipelineError(
                    "OCR failed on region ${region.regionIndex}: ${e.message}"
                )
            )
            throw e
        }
    }
}
