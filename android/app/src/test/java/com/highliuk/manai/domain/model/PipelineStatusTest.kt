package com.highliuk.manai.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class PipelineStatusTest {

    @Test
    fun `Queued color is orange`() {
        assertEquals(0x4DFF9800.toInt(), PipelineStatus.Queued.overlayColor)
    }

    @Test
    fun `Processing color is yellow`() {
        assertEquals(0x4DFFEB3B.toInt(), PipelineStatus.Processing.overlayColor)
    }

    @Test
    fun `Done color is green`() {
        assertEquals(0x4D4CAF50.toInt(), PipelineStatus.Done.overlayColor)
    }

    @Test
    fun `CacheHit color is blue`() {
        assertEquals(0x4D2196F3.toInt(), PipelineStatus.CacheHit.overlayColor)
    }

    @Test
    fun `Error color is red`() {
        val status = PipelineStatus.Error("fail")
        assertEquals(0x4DF44336.toInt(), status.overlayColor)
    }
}
