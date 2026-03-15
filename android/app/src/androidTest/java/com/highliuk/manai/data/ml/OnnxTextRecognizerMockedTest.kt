package com.highliuk.manai.data.ml

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OnnxTensorLike
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.graphics.Bitmap
import android.graphics.Color
import com.highliuk.manai.domain.ml.TextRegion
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.nio.FloatBuffer

class OnnxTextRecognizerMockedTest {

    private val ortEnv = OrtEnvironment.getEnvironment()
    private val sessionManager = mockk<OnnxSessionManager>(relaxed = true)
    private val encoderSession = mockk<OrtSession>(relaxed = true)
    private val decoderFirstSession = mockk<OrtSession>(relaxed = true)
    private val decoderWithPastSession = mockk<OrtSession>(relaxed = true)

    // vocab: 0=PAD 1=UNK 2=BOS 3=EOS 4=SEP 5=A 6=B 7=C
    private val vocab = listOf("PAD", "UNK", "BOS", "EOS", "SEP", "A", "B", "C")

    private lateinit var recognizer: OnnxTextRecognizer

    @Before
    fun setUp() {
        every { sessionManager.encoderSession } returns encoderSession
        every { sessionManager.decoderFirstSession } returns decoderFirstSession
        every { sessionManager.decoderWithPastSession } returns decoderWithPastSession
        every { sessionManager.ortEnv } returns ortEnv
        every { sessionManager.vocab } returns vocab

        recognizer = OnnxTextRecognizer(sessionManager)
    }

    @Test
    fun initialize_forcesLazyLoadOfAllSessions() = runTest {
        recognizer.initialize()

        verify { sessionManager.encoderSession }
        verify { sessionManager.decoderFirstSession }
        verify { sessionManager.decoderWithPastSession }
    }

    private fun logitsFor(token: Int): Array<Array<FloatArray>> {
        val logits = FloatArray(vocab.size) { -10f }
        logits[token] = 10f
        return arrayOf(arrayOf(logits))
    }

    private fun mockDecoderResult(tokenId: Int): OrtSession.Result {
        val result = mockk<OrtSession.Result>(relaxed = true)
        val logitsTensor = mockk<OnnxTensor>(relaxed = true)
        every { logitsTensor.value } returns logitsFor(tokenId)
        every { result[0] } returns logitsTensor
        // Create real OnnxTensors for past states (small 1x1 float tensors)
        for (i in 1..8) {
            val pastTensor = OnnxTensor.createTensor(
                ortEnv,
                FloatBuffer.wrap(floatArrayOf(0f)),
                longArrayOf(1, 1)
            )
            every { result[i] } returns pastTensor
        }
        return result
    }

    @Test
    fun recognize_producesTextFromPipeline() = runTest {
        val encoderResult = mockk<OrtSession.Result>(relaxed = true)
        val encoderOutput = OnnxTensor.createTensor(
            ortEnv, FloatBuffer.wrap(floatArrayOf(0f)), longArrayOf(1, 1)
        )
        every { encoderResult[0] } returns encoderOutput
        every { encoderSession.run(any<Map<String, OnnxTensor>>()) } returns encoderResult

        // First decoder step → token 5 (A)
        every { decoderFirstSession.run(any<Map<String, OnnxTensor>>()) } returns mockDecoderResult(5)

        // Decoder with past: step 2 → token 6 (B), step 3 → EOS (3)
        every {
            decoderWithPastSession.run(any<Map<String, OnnxTensorLike>>())
        } returnsMany listOf(mockDecoderResult(6), mockDecoderResult(3))

        val bitmap = Bitmap.createBitmap(400, 400, Bitmap.Config.ARGB_8888).apply {
            eraseColor(Color.WHITE)
        }
        val region = TextRegion(x1 = 10f, y1 = 10f, x2 = 200f, y2 = 100f, confidence = 0.9f)

        val result = recognizer.recognize(bitmap, region)

        assertEquals("AB", result.text)
        assertEquals(region, result.region)
    }

    @Test
    fun recognize_handlesImmediateEos() = runTest {
        val encoderResult = mockk<OrtSession.Result>(relaxed = true)
        val encoderOutput = OnnxTensor.createTensor(
            ortEnv, FloatBuffer.wrap(floatArrayOf(0f)), longArrayOf(1, 1)
        )
        every { encoderResult[0] } returns encoderOutput
        every { encoderSession.run(any<Map<String, OnnxTensor>>()) } returns encoderResult

        every { decoderFirstSession.run(any<Map<String, OnnxTensor>>()) } returns mockDecoderResult(3)

        val bitmap = Bitmap.createBitmap(400, 400, Bitmap.Config.ARGB_8888).apply {
            eraseColor(Color.BLACK)
        }
        val region = TextRegion(x1 = 0f, y1 = 0f, x2 = 100f, y2 = 50f, confidence = 0.8f)

        val result = recognizer.recognize(bitmap, region)

        assertEquals("", result.text)
    }

    @Test
    fun recognize_clampsRegionToImageBounds() = runTest {
        val encoderResult = mockk<OrtSession.Result>(relaxed = true)
        val encoderOutput = OnnxTensor.createTensor(
            ortEnv, FloatBuffer.wrap(floatArrayOf(0f)), longArrayOf(1, 1)
        )
        every { encoderResult[0] } returns encoderOutput
        every { encoderSession.run(any<Map<String, OnnxTensor>>()) } returns encoderResult

        every { decoderFirstSession.run(any<Map<String, OnnxTensor>>()) } returns mockDecoderResult(3)

        val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888).apply {
            eraseColor(Color.BLUE)
        }
        // Region extends beyond bitmap bounds
        val region = TextRegion(x1 = -10f, y1 = -10f, x2 = 200f, y2 = 200f, confidence = 0.7f)

        val result = recognizer.recognize(bitmap, region)

        assertEquals(region, result.region)
    }
}
