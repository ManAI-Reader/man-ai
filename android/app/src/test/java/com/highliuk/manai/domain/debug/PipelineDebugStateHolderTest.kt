package com.highliuk.manai.domain.debug

import app.cash.turbine.test
import com.highliuk.manai.domain.model.BalloonPipelineStatus
import com.highliuk.manai.domain.model.PagePipelineState
import com.highliuk.manai.domain.model.PipelineStatus
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class PipelineDebugStateHolderTest {

    private val holder = PipelineDebugStateHolder()

    @Test
    fun `initial state is empty map`() {
        assertEquals(emptyMap<Int, PagePipelineState>(), holder.states.value)
    }

    @Test
    fun `setPageStatus creates new entry if absent`() {
        holder.setPageStatus(0, PipelineStatus.Queued)

        val state = holder.states.value[0]!!
        assertEquals(PipelineStatus.Queued, state.pageStatus)
        assertEquals(0, state.pageIndex)
    }

    @Test
    fun `setPageStatus updates existing entry`() {
        holder.setPageStatus(0, PipelineStatus.Queued)
        holder.setPageStatus(0, PipelineStatus.Processing)

        assertEquals(PipelineStatus.Processing, holder.states.value[0]!!.pageStatus)
    }

    @Test
    fun `setBalloonStatus updates balloon within page`() {
        holder.setPageStatus(0, PipelineStatus.Done)
        holder.setBalloonStatus(0, 2, BalloonPipelineStatus.OcrQueued(1))

        assertEquals(
            BalloonPipelineStatus.OcrQueued(1),
            holder.states.value[0]!!.balloonStatuses[2]
        )
    }

    @Test
    fun `setBalloonStatuses sets multiple balloons at once`() {
        holder.setPageStatus(0, PipelineStatus.Done)
        holder.setBalloonStatuses(0, mapOf(
            0 to BalloonPipelineStatus.OcrQueued(1),
            1 to BalloonPipelineStatus.OcrQueued(2),
        ))

        val balloons = holder.states.value[0]!!.balloonStatuses
        assertEquals(BalloonPipelineStatus.OcrQueued(1), balloons[0])
        assertEquals(BalloonPipelineStatus.OcrQueued(2), balloons[1])
    }

    @Test
    fun `states flow emits updates`() = runTest {
        holder.states.test {
            assertEquals(emptyMap<Int, PagePipelineState>(), awaitItem())

            holder.setPageStatus(0, PipelineStatus.Queued)
            val updated = awaitItem()
            assertEquals(1, updated.size)
            assertEquals(PipelineStatus.Queued, updated[0]!!.pageStatus)
        }
    }
}
