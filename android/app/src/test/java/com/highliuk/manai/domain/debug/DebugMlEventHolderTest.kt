package com.highliuk.manai.domain.debug

import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class DebugMlEventHolderTest {

    private val holder = DebugMlEventHolder()

    @Test
    fun `emit sends event to flow`() = runTest {
        holder.events.test {
            holder.emit(DebugMlEvent.ModelLoading("detector"))
            assertEquals(DebugMlEvent.ModelLoading("detector"), awaitItem())
        }
    }

    @Test
    fun `emit sends error events`() = runTest {
        holder.events.test {
            holder.emit(DebugMlEvent.PipelineError("page 0: OOM"))
            assertEquals(DebugMlEvent.PipelineError("page 0: OOM"), awaitItem())
        }
    }

    @Test
    fun `emit sends model ready events`() = runTest {
        holder.events.test {
            holder.emit(DebugMlEvent.ModelReady("recognizer"))
            assertEquals(DebugMlEvent.ModelReady("recognizer"), awaitItem())
        }
    }
}
