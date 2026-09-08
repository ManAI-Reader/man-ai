package com.highliuk.manai.domain.llm

import org.junit.Assert.assertEquals
import org.junit.Test

class LlmEventTest {

    @Test
    fun `text delta carries its text`() {
        val event: LlmEvent = LlmEvent.TextDelta("こん")
        assertEquals("こん", (event as LlmEvent.TextDelta).text)
    }

    @Test
    fun `tool call carries id name and raw json arguments`() {
        val call = LlmToolCall(id = "call_1", name = "memory_read", arguments = """{"title":"kanji"}""")
        assertEquals("memory_read", call.name)
    }
}
