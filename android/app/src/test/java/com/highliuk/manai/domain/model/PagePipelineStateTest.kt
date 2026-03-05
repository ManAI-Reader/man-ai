package com.highliuk.manai.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class PagePipelineStateTest {

    @Test
    fun `default state has Queued page status and empty balloons`() {
        val state = PagePipelineState(pageIndex = 0)
        assertEquals(PipelineStatus.Queued, state.pageStatus)
        assertEquals(emptyMap<Int, BalloonPipelineStatus>(), state.balloonStatuses)
    }

    @Test
    fun `balloon statuses are indexed by regionIndex`() {
        val state = PagePipelineState(
            pageIndex = 1,
            pageStatus = PipelineStatus.Done,
            balloonStatuses = mapOf(
                0 to BalloonPipelineStatus.OcrQueued(1),
                3 to BalloonPipelineStatus.OcrProcessing,
            )
        )
        assertEquals(BalloonPipelineStatus.OcrQueued(1), state.balloonStatuses[0])
        assertEquals(BalloonPipelineStatus.OcrProcessing, state.balloonStatuses[3])
    }
}
