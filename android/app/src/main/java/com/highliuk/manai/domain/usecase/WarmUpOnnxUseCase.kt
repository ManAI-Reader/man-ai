package com.highliuk.manai.domain.usecase

import android.graphics.Bitmap
import com.highliuk.manai.domain.ml.TextDetector
import com.highliuk.manai.domain.ml.TextRecognizer
import com.highliuk.manai.domain.ml.TextRegion
import javax.inject.Inject

class WarmUpOnnxUseCase @Inject constructor(
    private val textDetector: TextDetector,
    private val textRecognizer: TextRecognizer,
) {
    suspend fun execute() {
        textDetector.initialize()
        textRecognizer.initialize()
        val bitmap = Bitmap.createBitmap(WARM_UP_SIZE, WARM_UP_SIZE, Bitmap.Config.ARGB_8888)
        try {
            textDetector.detect(bitmap)
            val dummyRegion = TextRegion(
                0f, 0f,
                WARM_UP_SIZE.toFloat(), WARM_UP_SIZE.toFloat(),
                1f,
            )
            textRecognizer.recognize(bitmap, dummyRegion)
        } finally {
            bitmap.recycle()
        }
    }

    companion object {
        private const val WARM_UP_SIZE = 32
    }
}
