package com.highliuk.manai.data.ml

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.graphics.Bitmap
import android.graphics.Color
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class OnnxTextDetectorMockedTest {

    private val ortEnv = OrtEnvironment.getEnvironment()
    private val sessionManager = mockk<OnnxSessionManager>(relaxed = true)
    private val detectorSession = mockk<OrtSession>(relaxed = true)
    private lateinit var detector: OnnxTextDetector

    @Before
    fun setUp() {
        every { sessionManager.detectorSession } returns detectorSession
        every { sessionManager.ortEnv } returns ortEnv
        every { detectorSession.inputNames } returns setOf("images")

        detector = spyk(OnnxTextDetector(sessionManager))
    }

    @Test
    fun initialize_forcesLazyLoadOfDetectorSession() = runTest {
        detector.initialize()

        verify { sessionManager.detectorSession }
    }

    @Test
    fun detect_returnsRegionsFromModelOutput() = runTest {
        // Simulate YOLO output: 1 detection at center (320,320) size 100x80, confidence 0.9
        val yoloOutput = arrayOf(arrayOf(
            floatArrayOf(320f),
            floatArrayOf(320f),
            floatArrayOf(100f),
            floatArrayOf(80f),
            floatArrayOf(0.9f),
        ))

        val result = mockk<OrtSession.Result>(relaxed = true)
        val outputTensor = mockk<OnnxTensor>(relaxed = true)
        every { result[0] } returns outputTensor
        every { outputTensor.value } returns yoloOutput
        every { detectorSession.run(any<Map<String, OnnxTensor>>()) } returns result

        val bitmap = Bitmap.createBitmap(640, 640, Bitmap.Config.ARGB_8888).apply {
            eraseColor(Color.WHITE)
        }
        val regions = detector.detect(bitmap)

        assertEquals(1, regions.size)
        assertEquals(0.9f, regions[0].confidence, 1e-4f)
    }

    @Test
    fun detect_returnsEmptyWhenAllBelowThreshold() = runTest {
        val yoloOutput = arrayOf(arrayOf(
            floatArrayOf(320f),
            floatArrayOf(320f),
            floatArrayOf(100f),
            floatArrayOf(80f),
            floatArrayOf(0.1f),
        ))

        val result = mockk<OrtSession.Result>(relaxed = true)
        val outputTensor = mockk<OnnxTensor>(relaxed = true)
        every { result[0] } returns outputTensor
        every { outputTensor.value } returns yoloOutput
        every { detectorSession.run(any<Map<String, OnnxTensor>>()) } returns result

        val bitmap = Bitmap.createBitmap(640, 640, Bitmap.Config.ARGB_8888).apply {
            eraseColor(Color.GRAY)
        }
        val regions = detector.detect(bitmap)

        assertTrue(regions.isEmpty())
    }

    @Test
    fun detect_appliesNmsToOverlappingDetections() = runTest {
        val yoloOutput = arrayOf(arrayOf(
            floatArrayOf(320f, 325f),
            floatArrayOf(320f, 325f),
            floatArrayOf(200f, 200f),
            floatArrayOf(200f, 200f),
            floatArrayOf(0.95f, 0.6f),
        ))

        val result = mockk<OrtSession.Result>(relaxed = true)
        val outputTensor = mockk<OnnxTensor>(relaxed = true)
        every { result[0] } returns outputTensor
        every { outputTensor.value } returns yoloOutput
        every { detectorSession.run(any<Map<String, OnnxTensor>>()) } returns result

        val bitmap = Bitmap.createBitmap(640, 640, Bitmap.Config.ARGB_8888).apply {
            eraseColor(Color.WHITE)
        }
        val regions = detector.detect(bitmap)

        assertEquals(1, regions.size)
        assertEquals(0.95f, regions[0].confidence, 1e-4f)
    }
}
