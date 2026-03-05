package com.highliuk.manai.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class BalloonPipelineStatusTest {

    @Test
    fun `OcrQueued has orange color and position`() {
        val status = BalloonPipelineStatus.OcrQueued(position = 3)
        assertEquals(0x4DFF9800.toInt(), status.overlayColor)
        assertEquals(3, status.position)
    }

    @Test
    fun `OcrProcessing has yellow color`() {
        assertEquals(0x4DFFEB3B.toInt(), BalloonPipelineStatus.OcrProcessing.overlayColor)
    }

    @Test
    fun `OcrDone has green color`() {
        assertEquals(0x4D4CAF50.toInt(), BalloonPipelineStatus.OcrDone.overlayColor)
    }

    @Test
    fun `OcrCacheHit has blue color`() {
        assertEquals(0x4D2196F3.toInt(), BalloonPipelineStatus.OcrCacheHit.overlayColor)
    }

    @Test
    fun `OcrError has red color and message`() {
        val status = BalloonPipelineStatus.OcrError("timeout")
        assertEquals(0x4DF44336.toInt(), status.overlayColor)
        assertEquals("timeout", status.message)
    }
}
