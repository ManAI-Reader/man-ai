package com.highliuk.manai.domain.usecase

import android.graphics.Bitmap
import com.highliuk.manai.domain.ml.OcrResult
import com.highliuk.manai.domain.ml.TextDetector
import com.highliuk.manai.domain.ml.TextRecognizer
import com.highliuk.manai.domain.ml.TextRegion
import com.highliuk.manai.domain.model.PageRegion
import com.highliuk.manai.domain.repository.OcrCacheRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProcessPageUseCaseTest {

    private val textDetector = mockk<TextDetector>(relaxed = true)
    private val textRecognizer = mockk<TextRecognizer>(relaxed = true)
    private val ocrCache = mockk<OcrCacheRepository>(relaxed = true)

    private val useCase = ProcessPageUseCase(textDetector, textRecognizer, ocrCache)

    private val bitmap = mockk<Bitmap>(relaxed = true)

    @Test
    fun `execute skips detection when cache has results`() = runTest {
        coEvery { ocrCache.hasDetectionResults(1L, 0) } returns true
        coEvery { ocrCache.getRegions(1L, 0) } returns listOf(
            PageRegion(0, 0.1f, 0.1f, 0.5f, 0.5f, 0.9f, "cached text")
        )

        useCase.execute(mangaId = 1L, pageIndex = 0, bitmap = bitmap, detectionOnly = true)

        coVerify(exactly = 0) { textDetector.detect(any()) }
    }

    @Test
    fun `execute runs detection and saves when cache empty`() = runTest {
        coEvery { ocrCache.hasDetectionResults(1L, 0) } returns false
        coEvery { bitmap.width } returns 640
        coEvery { bitmap.height } returns 480
        val regions = listOf(
            TextRegion(10f, 20f, 100f, 80f, 0.95f)
        )
        coEvery { textDetector.detect(bitmap) } returns regions

        useCase.execute(mangaId = 1L, pageIndex = 0, bitmap = bitmap, detectionOnly = true)

        coVerify { textDetector.detect(bitmap) }
        coVerify { ocrCache.saveDetectionResults(1L, 0, regions, 640, 480) }
    }

    @Test
    fun `execute runs OCR on regions with null ocrText`() = runTest {
        coEvery { ocrCache.hasDetectionResults(1L, 0) } returns true
        coEvery { ocrCache.getRegions(1L, 0) } returns listOf(
            PageRegion(0, 0.1f, 0.1f, 0.5f, 0.5f, 0.9f, null),
            PageRegion(1, 0.6f, 0.1f, 0.9f, 0.5f, 0.85f, "already done"),
        )
        coEvery { bitmap.width } returns 640
        coEvery { bitmap.height } returns 480
        coEvery {
            textRecognizer.recognize(bitmap, any())
        } returns OcrResult("recognized", TextRegion(0f, 0f, 1f, 1f, 1f))

        useCase.execute(mangaId = 1L, pageIndex = 0, bitmap = bitmap)

        coVerify(exactly = 1) { textRecognizer.recognize(bitmap, any()) }
        coVerify { ocrCache.saveOcrResult(1L, 0, 0, "recognized") }
        coVerify(exactly = 0) { ocrCache.saveOcrResult(1L, 0, 1, any()) }
    }

    @Test
    fun `execute processes priority region first`() = runTest {
        coEvery { ocrCache.hasDetectionResults(1L, 0) } returns true
        coEvery { ocrCache.getRegions(1L, 0) } returns listOf(
            PageRegion(0, 0.1f, 0.1f, 0.3f, 0.3f, 0.9f, null),
            PageRegion(1, 0.4f, 0.1f, 0.6f, 0.3f, 0.85f, null),
            PageRegion(2, 0.7f, 0.1f, 0.9f, 0.3f, 0.80f, null),
        )
        coEvery { bitmap.width } returns 640
        coEvery { bitmap.height } returns 480
        coEvery {
            textRecognizer.recognize(bitmap, any())
        } returns OcrResult("text", TextRegion(0f, 0f, 1f, 1f, 1f))

        val ocrOrder = mutableListOf<Int>()
        coEvery { ocrCache.saveOcrResult(1L, 0, any(), any()) } answers {
            ocrOrder.add(arg<Int>(2))
        }

        useCase.execute(mangaId = 1L, pageIndex = 0, bitmap = bitmap, priorityRegionIndex = 2)

        assertEquals(listOf(2, 0, 1), ocrOrder)
    }

    @Test
    fun `execute stops OCR when coroutine is cancelled between steps`() = runTest {
        coEvery { ocrCache.hasDetectionResults(1L, 0) } returns true
        coEvery { ocrCache.getRegions(1L, 0) } returns listOf(
            PageRegion(0, 0.1f, 0.1f, 0.3f, 0.3f, 0.9f, null),
            PageRegion(1, 0.4f, 0.1f, 0.6f, 0.3f, 0.85f, null),
            PageRegion(2, 0.7f, 0.1f, 0.9f, 0.3f, 0.80f, null),
        )
        coEvery { bitmap.width } returns 640
        coEvery { bitmap.height } returns 480

        val recognizedIndices = mutableListOf<Int>()
        coEvery { ocrCache.saveOcrResult(1L, 0, any(), any()) } answers {
            recognizedIndices.add(arg<Int>(2))
        }

        lateinit var pipelineJob: kotlinx.coroutines.Job
        var callCount = 0
        coEvery { textRecognizer.recognize(bitmap, any()) } coAnswers {
            callCount++
            if (callCount >= 2) {
                pipelineJob.cancel()
            }
            OcrResult("text", TextRegion(0f, 0f, 1f, 1f, 1f))
        }

        pipelineJob = launch {
            useCase.execute(mangaId = 1L, pageIndex = 0, bitmap = bitmap)
        }
        pipelineJob.join()

        assertTrue("Should process fewer than 3 regions", recognizedIndices.size < 3)
    }

    @Test(expected = CancellationException::class)
    fun `CancellationException during detection propagates without being swallowed`() = runTest {
        coEvery { ocrCache.hasDetectionResults(1L, 0) } returns false
        coEvery { textDetector.detect(bitmap) } throws CancellationException("job cancelled")

        useCase.execute(mangaId = 1L, pageIndex = 0, bitmap = bitmap)
    }

    @Test(expected = CancellationException::class)
    fun `CancellationException during OCR propagates without being swallowed`() = runTest {
        coEvery { ocrCache.hasDetectionResults(1L, 0) } returns true
        coEvery { ocrCache.getRegions(1L, 0) } returns listOf(
            PageRegion(0, 0.1f, 0.1f, 0.5f, 0.5f, 0.9f, null),
        )
        coEvery { bitmap.width } returns 640
        coEvery { bitmap.height } returns 480
        coEvery { textRecognizer.recognize(bitmap, any()) } throws CancellationException("job cancelled")

        useCase.execute(mangaId = 1L, pageIndex = 0, bitmap = bitmap)
    }
}
