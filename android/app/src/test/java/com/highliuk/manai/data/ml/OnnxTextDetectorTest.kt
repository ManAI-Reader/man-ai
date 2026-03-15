package com.highliuk.manai.data.ml

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OnnxTextDetectorTest {

    @Test
    fun `computeLetterboxParams returns correct scale and padding for landscape image`() {
        val result = OnnxTextDetector.computeLetterboxParams(
            origWidth = 1280,
            origHeight = 720,
            inputSize = 640,
        )

        assertEquals(1280, result.origWidth)
        assertEquals(720, result.origHeight)
        assertEquals(0.5f, result.scale, 1e-4f)
        assertEquals(640, result.newWidth)
        assertEquals(360, result.newHeight)
        assertEquals(0, result.padLeft)
        assertEquals(140, result.padTop)
    }

    @Test
    fun `computeLetterboxParams returns correct values for portrait image`() {
        val result = OnnxTextDetector.computeLetterboxParams(
            origWidth = 480,
            origHeight = 960,
            inputSize = 640,
        )

        assertEquals(480, result.origWidth)
        assertEquals(960, result.origHeight)
        assertEquals(0.6667f, result.scale, 1e-3f)
        assertEquals(320, result.newWidth)
        assertEquals(640, result.newHeight)
        assertEquals(160, result.padLeft)
        assertEquals(0, result.padTop)
    }

    @Test
    fun `computeLetterboxParams returns correct values for square image`() {
        val result = OnnxTextDetector.computeLetterboxParams(
            origWidth = 640,
            origHeight = 640,
            inputSize = 640,
        )

        assertEquals(640, result.origWidth)
        assertEquals(640, result.origHeight)
        assertEquals(1.0f, result.scale, 1e-4f)
        assertEquals(640, result.newWidth)
        assertEquals(640, result.newHeight)
        assertEquals(0, result.padLeft)
        assertEquals(0, result.padTop)
    }

    @Test
    fun `postprocessYoloOutput filters boxes below confidence threshold`() {
        val output = arrayOf(
            floatArrayOf(320f),
            floatArrayOf(320f),
            floatArrayOf(100f),
            floatArrayOf(100f),
            floatArrayOf(0.3f),
        )
        val letterbox = OnnxTextDetector.LetterboxParams(
            origWidth = 640, origHeight = 640,
            scale = 1.0f, newWidth = 640, newHeight = 640,
            padLeft = 0, padTop = 0,
        )

        val result = OnnxTextDetector.postprocessYoloOutput(
            output = output,
            letterbox = letterbox,
            confThreshold = 0.5f,
            iouThreshold = 0.45f,
        )

        assertTrue(result.isEmpty())
    }

    @Test
    fun `postprocessYoloOutput converts center coords to corner coords and removes padding`() {
        // Landscape 1280x720 → scale=0.5, padLeft=0, padTop=140
        // Model-space box: center(200,240) size(100,80) score=0.9
        // Corner in model: x1=150 y1=200 x2=250 y2=280
        // Unpad+unscale: x1=(150-0)*2=300 y1=(200-140)*2=120 x2=500 y2=280
        val output = arrayOf(
            floatArrayOf(200f),
            floatArrayOf(240f),
            floatArrayOf(100f),
            floatArrayOf(80f),
            floatArrayOf(0.9f),
        )
        val letterbox = OnnxTextDetector.LetterboxParams(
            origWidth = 1280, origHeight = 720,
            scale = 0.5f, newWidth = 640, newHeight = 360,
            padLeft = 0, padTop = 140,
        )

        val result = OnnxTextDetector.postprocessYoloOutput(
            output = output,
            letterbox = letterbox,
            confThreshold = 0.5f,
            iouThreshold = 0.45f,
        )

        assertEquals(1, result.size)
        val region = result[0]
        assertEquals(300f, region.x1, 1e-2f)
        assertEquals(120f, region.y1, 1e-2f)
        assertEquals(500f, region.x2, 1e-2f)
        assertEquals(280f, region.y2, 1e-2f)
        assertEquals(0.9f, region.confidence, 1e-4f)
    }

    @Test
    fun `postprocessYoloOutput clamps coordinates to image bounds`() {
        // Square 640x640, scale=1, no pad. Box near corner overflows.
        // center(10,10) size(100,100) → x1=-40 y1=-40 x2=60 y2=60
        // Clamped: x1=0 y1=0 x2=60 y2=60
        val output = arrayOf(
            floatArrayOf(10f),
            floatArrayOf(10f),
            floatArrayOf(100f),
            floatArrayOf(100f),
            floatArrayOf(0.8f),
        )
        val letterbox = OnnxTextDetector.LetterboxParams(
            origWidth = 640, origHeight = 640,
            scale = 1.0f, newWidth = 640, newHeight = 640,
            padLeft = 0, padTop = 0,
        )

        val result = OnnxTextDetector.postprocessYoloOutput(
            output = output,
            letterbox = letterbox,
            confThreshold = 0.5f,
            iouThreshold = 0.45f,
        )

        assertEquals(1, result.size)
        val region = result[0]
        assertEquals(0f, region.x1, 1e-2f)
        assertEquals(0f, region.y1, 1e-2f)
        assertEquals(60f, region.x2, 1e-2f)
        assertEquals(60f, region.y2, 1e-2f)
    }

    @Test
    fun `postprocessYoloOutput returns empty for no predictions`() {
        val output = arrayOf(
            floatArrayOf(),
            floatArrayOf(),
            floatArrayOf(),
            floatArrayOf(),
            floatArrayOf(),
        )
        val letterbox = OnnxTextDetector.LetterboxParams(
            origWidth = 640, origHeight = 640,
            scale = 1.0f, newWidth = 640, newHeight = 640,
            padLeft = 0, padTop = 0,
        )

        val result = OnnxTextDetector.postprocessYoloOutput(
            output = output,
            letterbox = letterbox,
            confThreshold = 0.5f,
            iouThreshold = 0.45f,
        )

        assertTrue(result.isEmpty())
    }

    @Test
    fun `postprocessYoloOutput keeps multiple non-overlapping detections`() {
        // Two boxes far apart — both should survive NMS
        val output = arrayOf(
            floatArrayOf(100f, 500f),
            floatArrayOf(100f, 500f),
            floatArrayOf(50f, 50f),
            floatArrayOf(50f, 50f),
            floatArrayOf(0.9f, 0.8f),
        )
        val letterbox = OnnxTextDetector.LetterboxParams(
            origWidth = 640, origHeight = 640,
            scale = 1.0f, newWidth = 640, newHeight = 640,
            padLeft = 0, padTop = 0,
        )

        val result = OnnxTextDetector.postprocessYoloOutput(
            output = output,
            letterbox = letterbox,
            confThreshold = 0.5f,
            iouThreshold = 0.45f,
        )

        assertEquals(2, result.size)
    }

    @Test
    fun `postprocessYoloOutput applies NMS to filter overlapping boxes`() {
        val output = arrayOf(
            floatArrayOf(320f, 330f),
            floatArrayOf(320f, 330f),
            floatArrayOf(200f, 200f),
            floatArrayOf(200f, 200f),
            floatArrayOf(0.9f, 0.6f),
        )
        val letterbox = OnnxTextDetector.LetterboxParams(
            origWidth = 640, origHeight = 640,
            scale = 1.0f, newWidth = 640, newHeight = 640,
            padLeft = 0, padTop = 0,
        )

        val result = OnnxTextDetector.postprocessYoloOutput(
            output = output,
            letterbox = letterbox,
            confThreshold = 0.5f,
            iouThreshold = 0.45f,
        )

        assertEquals(1, result.size)
        assertEquals(0.9f, result[0].confidence, 1e-4f)
    }
}
