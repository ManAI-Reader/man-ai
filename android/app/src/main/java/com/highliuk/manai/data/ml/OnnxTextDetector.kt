package com.highliuk.manai.data.ml

import ai.onnxruntime.OnnxTensor
import android.graphics.Bitmap
import com.highliuk.manai.domain.ml.TextDetector
import com.highliuk.manai.domain.ml.TextRegion
import java.nio.FloatBuffer
import javax.inject.Inject
import kotlin.math.min
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class OnnxTextDetector @Inject constructor(
    private val sessionManager: OnnxSessionManager,
) : TextDetector {

    override suspend fun initialize() = withContext(Dispatchers.Default) {
        sessionManager.detectorSession // force lazy load
        Unit
    }

    override suspend fun detect(bitmap: Bitmap): List<TextRegion> = withContext(Dispatchers.Default) {
        val letterbox = computeLetterboxParams(bitmap.width, bitmap.height, DETECTOR_INPUT_SIZE)
        val inputTensor = preprocessDetector(bitmap, letterbox)

        try {
            val inputName = sessionManager.detectorSession.inputNames.first()
            val results = sessionManager.detectorSession.run(mapOf(inputName to inputTensor))

            try {
                @Suppress("UNCHECKED_CAST")
                val output = (results[0].value as Array<Array<FloatArray>>)[0]
                postprocessYoloOutput(output, letterbox, CONF_THRESHOLD, IOU_THRESHOLD)
            } finally {
                results.close()
            }
        } finally {
            inputTensor.close()
        }
    }

    internal fun preprocessDetector(src: Bitmap, letterbox: LetterboxParams): OnnxTensor {
        val planeSize = DETECTOR_INPUT_SIZE * DETECTOR_INPUT_SIZE
        val floats = FloatArray(3 * planeSize)

        val origWidth = src.width
        val origHeight = src.height
        val srcPixels = IntArray(origWidth * origHeight)
        src.getPixels(srcPixels, 0, origWidth, 0, 0, origWidth, origHeight)

        val padRight = letterbox.padLeft + letterbox.newWidth
        val padBottom = letterbox.padTop + letterbox.newHeight
        val invScale = 1f / letterbox.scale
        val inv255 = 1f / 255f

        var outIdx = 0
        for (outY in 0 until DETECTOR_INPUT_SIZE) {
            val inPadY = outY < letterbox.padTop || outY >= padBottom
            val srcY = if (inPadY) -1 else ((outY - letterbox.padTop) * invScale).toInt().coerceIn(0, origHeight - 1)
            val srcRowOffset = srcY * origWidth

            for (outX in 0 until DETECTOR_INPUT_SIZE) {
                if (inPadY || outX < letterbox.padLeft || outX >= padRight) {
                    floats[outIdx] = PAD_VALUE
                    floats[planeSize + outIdx] = PAD_VALUE
                    floats[2 * planeSize + outIdx] = PAD_VALUE
                } else {
                    val srcX = ((outX - letterbox.padLeft) * invScale).toInt().coerceIn(0, origWidth - 1)
                    val p = srcPixels[srcRowOffset + srcX]
                    floats[outIdx] = (p shr 16 and 0xFF) * inv255
                    floats[planeSize + outIdx] = (p shr 8 and 0xFF) * inv255
                    floats[2 * planeSize + outIdx] = (p and 0xFF) * inv255
                }
                outIdx++
            }
        }

        return OnnxTensor.createTensor(
            sessionManager.ortEnv,
            FloatBuffer.wrap(floats),
            longArrayOf(1, 3, DETECTOR_INPUT_SIZE.toLong(), DETECTOR_INPUT_SIZE.toLong()),
        )
    }

    data class LetterboxParams(
        val origWidth: Int,
        val origHeight: Int,
        val scale: Float,
        val newWidth: Int,
        val newHeight: Int,
        val padLeft: Int,
        val padTop: Int,
    )

    companion object {
        private const val DETECTOR_INPUT_SIZE = 640
        private const val CONF_THRESHOLD = 0.5f
        private const val IOU_THRESHOLD = 0.45f
        private const val PAD_VALUE = 0.4471f // 114/255

        fun computeLetterboxParams(
            origWidth: Int,
            origHeight: Int,
            inputSize: Int = DETECTOR_INPUT_SIZE,
        ): LetterboxParams {
            val scale = min(inputSize.toFloat() / origWidth, inputSize.toFloat() / origHeight)
            val newWidth = (origWidth * scale).toInt()
            val newHeight = (origHeight * scale).toInt()
            val padLeft = (inputSize - newWidth) / 2
            val padTop = (inputSize - newHeight) / 2
            return LetterboxParams(origWidth, origHeight, scale, newWidth, newHeight, padLeft, padTop)
        }

        fun postprocessYoloOutput(
            output: Array<FloatArray>,
            letterbox: LetterboxParams,
            confThreshold: Float,
            iouThreshold: Float,
        ): List<TextRegion> {
            val numPredictions = output[0].size
            val proposals = mutableListOf<FloatArray>()
            val invScale = 1f / letterbox.scale

            for (i in 0 until numPredictions) {
                val score = output[4][i]
                if (score > confThreshold) {
                    val xCenter = output[0][i]
                    val yCenter = output[1][i]
                    val width = output[2][i]
                    val height = output[3][i]

                    var x1 = (xCenter - width / 2 - letterbox.padLeft) * invScale
                    var y1 = (yCenter - height / 2 - letterbox.padTop) * invScale
                    var x2 = (xCenter + width / 2 - letterbox.padLeft) * invScale
                    var y2 = (yCenter + height / 2 - letterbox.padTop) * invScale

                    x1 = x1.coerceIn(0f, letterbox.origWidth.toFloat())
                    y1 = y1.coerceIn(0f, letterbox.origHeight.toFloat())
                    x2 = x2.coerceIn(0f, letterbox.origWidth.toFloat())
                    y2 = y2.coerceIn(0f, letterbox.origHeight.toFloat())

                    proposals.add(floatArrayOf(x1, y1, x2, y2, score))
                }
            }

            return NmsProcessor.applyNms(proposals, iouThreshold).map { box ->
                TextRegion(
                    x1 = box[0],
                    y1 = box[1],
                    x2 = box[2],
                    y2 = box[3],
                    confidence = box[4],
                )
            }
        }
    }
}
