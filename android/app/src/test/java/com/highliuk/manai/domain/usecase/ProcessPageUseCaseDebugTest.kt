package com.highliuk.manai.domain.usecase

import android.graphics.Bitmap
import app.cash.turbine.test
import com.highliuk.manai.domain.debug.DebugMlEventHolder
import com.highliuk.manai.domain.debug.PipelineDebugStateHolder
import com.highliuk.manai.domain.ml.OcrResult
import com.highliuk.manai.domain.ml.TextDetector
import com.highliuk.manai.domain.ml.TextRecognizer
import com.highliuk.manai.domain.ml.TextRegion
import com.highliuk.manai.domain.model.BalloonPipelineStatus
import com.highliuk.manai.domain.model.PageRegion
import com.highliuk.manai.domain.model.PipelineStatus
import com.highliuk.manai.domain.repository.OcrCacheRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class ProcessPageUseCaseDebugTest {

    private val textDetector = mockk<TextDetector>(relaxed = true)
    private val textRecognizer = mockk<TextRecognizer>(relaxed = true)
    private val ocrCache = mockk<OcrCacheRepository>(relaxed = true)
    private val debugStateHolder = PipelineDebugStateHolder()
    private val debugEventHolder = DebugMlEventHolder()
    private val bitmap = mockk<Bitmap>(relaxed = true)
    private val dummyRegion = TextRegion(0f, 0f, 1f, 1f, 1f)

    private lateinit var useCase: ProcessPageUseCase

    @Before
    fun setUp() {
        every { bitmap.width } returns 100
        every { bitmap.height } returns 100
        useCase = ProcessPageUseCase(
            textDetector, textRecognizer, ocrCache, debugStateHolder, debugEventHolder
        )
    }

    @Test
    fun `emits Processing then Done for page when detection runs`() = runTest {
        coEvery { ocrCache.hasDetectionResults(1L, 0) } returns false
        coEvery { textDetector.detect(bitmap) } returns emptyList()
        coEvery { ocrCache.getRegions(1L, 0) } returns emptyList()

        useCase.execute(1L, 0, bitmap)

        assertEquals(PipelineStatus.Done, debugStateHolder.states.value[0]?.pageStatus)
    }

    @Test
    fun `emits CacheHit for page when detection is cached`() = runTest {
        coEvery { ocrCache.hasDetectionResults(1L, 0) } returns true
        coEvery { ocrCache.getRegions(1L, 0) } returns emptyList()

        useCase.execute(1L, 0, bitmap)

        assertEquals(PipelineStatus.CacheHit, debugStateHolder.states.value[0]?.pageStatus)
    }

    @Test
    fun `emits balloon queue positions then OcrDone`() = runTest {
        coEvery { ocrCache.hasDetectionResults(1L, 0) } returns true
        coEvery { ocrCache.getRegions(1L, 0) } returns listOf(
            PageRegion(0, 0.1f, 0.1f, 0.5f, 0.5f, 0.9f, null),
            PageRegion(1, 0.5f, 0.5f, 0.9f, 0.9f, 0.8f, null),
        )
        coEvery { textRecognizer.recognize(any(), any()) } returns OcrResult("text", dummyRegion)

        useCase.execute(1L, 0, bitmap)

        val balloons = debugStateHolder.states.value[0]!!.balloonStatuses
        assertEquals(BalloonPipelineStatus.OcrDone, balloons[0])
        assertEquals(BalloonPipelineStatus.OcrDone, balloons[1])
    }

    @Test
    fun `emits OcrCacheHit for balloons already with ocrText`() = runTest {
        coEvery { ocrCache.hasDetectionResults(1L, 0) } returns true
        coEvery { ocrCache.getRegions(1L, 0) } returns listOf(
            PageRegion(0, 0.1f, 0.1f, 0.5f, 0.5f, 0.9f, "already done"),
        )

        useCase.execute(1L, 0, bitmap)

        val balloons = debugStateHolder.states.value[0]!!.balloonStatuses
        assertEquals(BalloonPipelineStatus.OcrCacheHit, balloons[0])
    }

    @Test
    fun `emits OcrError when recognizer throws`() = runTest {
        coEvery { ocrCache.hasDetectionResults(1L, 0) } returns true
        coEvery { ocrCache.getRegions(1L, 0) } returns listOf(
            PageRegion(0, 0.1f, 0.1f, 0.5f, 0.5f, 0.9f, null),
        )
        coEvery { textRecognizer.recognize(any(), any()) } throws RuntimeException("OOM")

        try { useCase.execute(1L, 0, bitmap) } catch (_: RuntimeException) { }

        val balloons = debugStateHolder.states.value[0]!!.balloonStatuses
        assertEquals(BalloonPipelineStatus.OcrError("OOM"), balloons[0])
    }

    @Test
    fun `all balloons are Done after full execution`() = runTest {
        coEvery { ocrCache.hasDetectionResults(1L, 0) } returns true
        coEvery { ocrCache.getRegions(1L, 0) } returns listOf(
            PageRegion(0, 0.1f, 0.1f, 0.5f, 0.5f, 0.9f, null),
            PageRegion(1, 0.5f, 0.5f, 0.9f, 0.9f, 0.8f, null),
            PageRegion(2, 0.1f, 0.5f, 0.5f, 0.9f, 0.7f, null),
        )
        coEvery { textRecognizer.recognize(any(), any()) } returns OcrResult("text", dummyRegion)

        useCase.execute(1L, 0, bitmap)

        val balloons = debugStateHolder.states.value[0]!!.balloonStatuses
        assertEquals(BalloonPipelineStatus.OcrDone, balloons[0])
        assertEquals(BalloonPipelineStatus.OcrDone, balloons[1])
        assertEquals(BalloonPipelineStatus.OcrDone, balloons[2])
    }

    @Test
    fun `keeps Done for page when re-executing with cached detection`() = runTest {
        // First execution: detection runs, page becomes Done
        coEvery { ocrCache.hasDetectionResults(1L, 0) } returns false
        coEvery { textDetector.detect(bitmap) } returns emptyList()
        coEvery { ocrCache.getRegions(1L, 0) } returns emptyList()
        useCase.execute(1L, 0, bitmap)
        assertEquals(PipelineStatus.Done, debugStateHolder.states.value[0]?.pageStatus)

        // Second execution: detection is cached, should stay Done not become CacheHit
        coEvery { ocrCache.hasDetectionResults(1L, 0) } returns true
        useCase.execute(1L, 0, bitmap)
        assertEquals(PipelineStatus.Done, debugStateHolder.states.value[0]?.pageStatus)
    }

    @Test
    fun `keeps OcrDone for balloons when re-executing with cached OCR`() = runTest {
        // First execution: OCR runs, balloons become Done
        coEvery { ocrCache.hasDetectionResults(1L, 0) } returns true
        coEvery { ocrCache.getRegions(1L, 0) } returns listOf(
            PageRegion(0, 0.1f, 0.1f, 0.5f, 0.5f, 0.9f, null),
        )
        coEvery { textRecognizer.recognize(any(), any()) } returns OcrResult("text", dummyRegion)
        useCase.execute(1L, 0, bitmap)
        assertEquals(BalloonPipelineStatus.OcrDone, debugStateHolder.states.value[0]!!.balloonStatuses[0])

        // Second execution: OCR is cached (ocrText != null), should stay Done
        coEvery { ocrCache.getRegions(1L, 0) } returns listOf(
            PageRegion(0, 0.1f, 0.1f, 0.5f, 0.5f, 0.9f, "text"),
        )
        useCase.execute(1L, 0, bitmap)
        assertEquals(BalloonPipelineStatus.OcrDone, debugStateHolder.states.value[0]!!.balloonStatuses[0])
    }

    @Test
    fun `emits PipelineError event when detection fails`() = runTest {
        coEvery { ocrCache.hasDetectionResults(1L, 0) } returns false
        coEvery { textDetector.detect(bitmap) } throws RuntimeException("OOM")

        debugEventHolder.events.test {
            try { useCase.execute(1L, 0, bitmap) } catch (_: RuntimeException) { }
            assertEquals(
                "Pipeline error: Detection failed on page 0: OOM",
                awaitItem().toastMessage
            )
        }
    }

    @Test
    fun `emits PipelineError event when OCR fails`() = runTest {
        coEvery { ocrCache.hasDetectionResults(1L, 0) } returns true
        coEvery { ocrCache.getRegions(1L, 0) } returns listOf(
            PageRegion(0, 0.1f, 0.1f, 0.5f, 0.5f, 0.9f, null),
        )
        coEvery { textRecognizer.recognize(any(), any()) } throws RuntimeException("timeout")

        debugEventHolder.events.test {
            try { useCase.execute(1L, 0, bitmap) } catch (_: RuntimeException) { }
            assertEquals(
                "Pipeline error: OCR failed on region 0: timeout",
                awaitItem().toastMessage
            )
        }
    }
}
