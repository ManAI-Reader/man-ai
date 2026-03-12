package com.highliuk.manai.data.ml

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OnnxTensorLike
import android.graphics.Bitmap
import com.highliuk.manai.domain.ml.OcrResult
import com.highliuk.manai.domain.ml.TextRecognizer
import com.highliuk.manai.domain.ml.TextRegion
import java.nio.FloatBuffer
import java.nio.LongBuffer
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class OnnxTextRecognizer @Inject constructor(
    private val sessionManager: OnnxSessionManager,
) : TextRecognizer {

    override suspend fun initialize() = withContext(Dispatchers.Default) {
        sessionManager.encoderSession
        sessionManager.decoderFirstSession
        sessionManager.decoderWithPastSession
        Unit
    }

    override suspend fun recognize(bitmap: Bitmap, region: TextRegion): OcrResult =
        withContext(Dispatchers.Default) {
            val x1 = region.x1.toInt().coerceIn(0, bitmap.width - 1)
            val y1 = region.y1.toInt().coerceIn(0, bitmap.height - 1)
            val x2 = region.x2.toInt().coerceIn(0, bitmap.width - 1)
            val y2 = region.y2.toInt().coerceIn(0, bitmap.height - 1)
            val cropW = (x2 - x1).coerceAtLeast(1)
            val cropH = (y2 - y1).coerceAtLeast(1)

            val crop = Bitmap.createBitmap(bitmap, x1, y1, cropW, cropH)
            val scaled = Bitmap.createScaledBitmap(crop, OCR_INPUT_SIZE, OCR_INPUT_SIZE, true)

            val pixels = IntArray(OCR_INPUT_SIZE * OCR_INPUT_SIZE)
            scaled.getPixels(pixels, 0, OCR_INPUT_SIZE, 0, 0, OCR_INPUT_SIZE, OCR_INPUT_SIZE)

            val floats = preprocessOcrPixels(pixels, OCR_INPUT_SIZE, OCR_INPUT_SIZE)
            val pixelValues = OnnxTensor.createTensor(
                sessionManager.ortEnv,
                FloatBuffer.wrap(floats),
                longArrayOf(1, 3, OCR_INPUT_SIZE.toLong(), OCR_INPUT_SIZE.toLong()),
            )

            try {
                val text = runInference(pixelValues)
                OcrResult(text = text, region = region)
            } finally {
                pixelValues.close()
            }
        }

    private fun runInference(pixelValues: OnnxTensor): String {
        val encoderResult = sessionManager.encoderSession.run(mapOf("pixel_values" to pixelValues))
        try {
            val encoderOutput = encoderResult[0] as OnnxTensor
            return runDecoderGreedy(encoderOutput)
        } finally {
            encoderResult.close()
        }
    }

    @Suppress("NestedBlockDepth")
    private fun runDecoderGreedy(encoderOutput: OnnxTensor): String {
        val tokens = mutableListOf<Int>()
        var tokenId = BOS_TOKEN.toLong()

        var currentResult = sessionManager.decoderFirstSession.run(
            mapOf(
                "input_ids" to createInputIds(tokenId),
                "encoder_hidden_states" to encoderOutput,
            ),
        )

        try {
            for (step in 0 until MAX_DECODE_LENGTH) {
                @Suppress("UNCHECKED_CAST")
                val logits = currentResult[0].value as Array<Array<FloatArray>>
                tokenId = logits[0].last().indices.maxBy { logits[0].last()[it] }.toLong()

                if (tokenId == EOS_TOKEN.toLong()) break
                tokens.add(tokenId.toInt())

                val inputs = mutableMapOf<String, OnnxTensorLike>(
                    "input_ids" to createInputIds(tokenId),
                    "encoder_hidden_states" to encoderOutput,
                )
                for (p in 0 until PAST_KEY_COUNT) {
                    inputs["past_$p"] = currentResult[p + 1] as OnnxTensor
                }
                val nextResult = sessionManager.decoderWithPastSession.run(inputs)
                currentResult.close()
                currentResult = nextResult
            }
        } finally {
            currentResult.close()
        }

        return decodeTokens(tokens, sessionManager.vocab)
    }

    private fun createInputIds(id: Long): OnnxTensor {
        return OnnxTensor.createTensor(
            sessionManager.ortEnv,
            LongBuffer.wrap(longArrayOf(id)),
            longArrayOf(1, 1),
        )
    }

    companion object {
        private const val BOS_TOKEN = 2
        private const val EOS_TOKEN = 3
        private const val FIRST_REAL_TOKEN = 5
        private const val MAX_DECODE_LENGTH = 40
        private const val OCR_INPUT_SIZE = 224
        private const val PAST_KEY_COUNT = 8

        fun preprocessOcrPixels(pixels: IntArray, width: Int, height: Int): FloatArray {
            val planeSize = width * height
            val floats = FloatArray(3 * planeSize)

            for (i in pixels.indices) {
                val p = pixels[i]
                val r = (p shr 16 and 0xFF) / 255f
                val g = (p shr 8 and 0xFF) / 255f
                val b = (p and 0xFF) / 255f
                floats[i] = (r - 0.5f) / 0.5f
                floats[planeSize + i] = (g - 0.5f) / 0.5f
                floats[2 * planeSize + i] = (b - 0.5f) / 0.5f
            }

            return floats
        }

        fun decodeTokens(tokens: List<Int>, vocab: List<String>): String {
            return tokens
                .filter { it >= FIRST_REAL_TOKEN }
                .map { vocab.getOrElse(it) { "" } }
                .joinToString("")
        }

        fun greedyDecode(
            decoderStep: (tokenId: Int, step: Int) -> FloatArray,
            vocab: List<String>,
        ): String {
            val tokens = mutableListOf<Int>()
            var tokenId = BOS_TOKEN

            for (step in 0 until MAX_DECODE_LENGTH) {
                val logits = decoderStep(tokenId, step)
                tokenId = logits.indices.maxBy { logits[it] }
                if (tokenId == EOS_TOKEN) break
                tokens.add(tokenId)
            }

            return decodeTokens(tokens, vocab)
        }
    }
}
