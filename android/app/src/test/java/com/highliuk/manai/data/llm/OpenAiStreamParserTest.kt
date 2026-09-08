package com.highliuk.manai.data.llm

import com.highliuk.manai.domain.logging.Logger
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OpenAiStreamParserTest {

    @Test
    fun `parses content delta from data line`() {
        val line = """data: {"choices":[{"delta":{"content":"こん"},"finish_reason":null}]}"""
        val chunk = OpenAiStreamParser.parseDataLine(line)
        assertEquals("こん", chunk?.contentDelta)
        assertNull(chunk?.finishReason)
    }

    @Test
    fun `returns null for DONE sentinel, empty and non-data lines`() {
        assertNull(OpenAiStreamParser.parseDataLine("data: [DONE]"))
        assertNull(OpenAiStreamParser.parseDataLine(""))
        assertNull(OpenAiStreamParser.parseDataLine(": keep-alive"))
    }

    @Test
    fun `parses finish reason`() {
        val line = """data: {"choices":[{"delta":{},"finish_reason":"stop"}]}"""
        assertEquals("stop", OpenAiStreamParser.parseDataLine(line)?.finishReason)
    }

    @Test
    fun `parses deepseek reasoning chunk without leaking reasoning_content into visible text`() {
        // Real fixture captured from api.deepseek.com: content is null while the
        // model streams its hidden reasoning through the reasoning_content field.
        val line = "data: " +
            """{"id":"425128f1-49fe-4d6f-b5ba-ff9b19acfe72","object":"chat.completion.chunk",""" +
            """"created":1787551589,"model":"deepseek-v4-flash",""" +
            """"system_fingerprint":"a26a7955944dc5c60445bff77fac9c8e",""" +
            """"choices":[{"index":0,"delta":{"content":null,"reasoning_content":"We"},""" +
            """"logprobs":null,"finish_reason":null}]}"""

        val chunk = OpenAiStreamParser.parseDataLine(line)

        assertNotNull(chunk)
        assertNull(chunk?.contentDelta)
        assertTrue(chunk?.toolCallDeltas.orEmpty().isEmpty())
        assertNull(chunk?.finishReason)
    }

    @Test
    fun `returns null on malformed json`() {
        assertNull(OpenAiStreamParser.parseDataLine("data: {not json"))
    }

    @Test
    fun `parses data line without space after colon`() {
        val line = """data:{"choices":[{"delta":{"content":"hi"},"finish_reason":null}]}"""
        assertEquals("hi", OpenAiStreamParser.parseDataLine(line)?.contentDelta)
    }

    @Test
    fun `logs malformed payload through provided logger`() {
        val logger = mockk<Logger>(relaxed = true)

        assertNull(OpenAiStreamParser.parseDataLine("data: {not json", logger))

        verify { logger.e("OpenAiStreamParser", match { it.contains("{not json") }) }
    }

    @Test
    fun `truncates logged malformed payload to 200 chars`() {
        val logger = mockk<Logger>(relaxed = true)
        val payload = "{" + "x".repeat(300)

        assertNull(OpenAiStreamParser.parseDataLine("data: $payload", logger))

        verify {
            logger.e(
                "OpenAiStreamParser",
                match { message ->
                    assertTrue(message.contains("{" + "x".repeat(199)))
                    assertFalse(message.contains("x".repeat(200)))
                    true
                },
            )
        }
    }

    @Test
    fun `does not log valid or sentinel lines`() {
        val logger = mockk<Logger>(relaxed = true)

        OpenAiStreamParser.parseDataLine("data: [DONE]", logger)
        OpenAiStreamParser.parseDataLine(
            """data: {"choices":[{"delta":{"content":"ok"},"finish_reason":null}]}""",
            logger,
        )

        verify(exactly = 0) { logger.e(any(), any()) }
    }

    @Test
    fun `accumulator merges fragmented tool call arguments by index`() {
        val acc = ToolCallAccumulator()
        acc.add(
            listOf(ToolCallDelta(index = 0, id = "call_1", name = "memory_read", argumentsFragment = """{"ti"""))
        )
        acc.add(
            listOf(ToolCallDelta(index = 0, id = null, name = null, argumentsFragment = """tle":"kanji"}"""))
        )
        val calls = acc.build()
        assertEquals(1, calls.size)
        assertEquals("call_1", calls[0].id)
        assertEquals("memory_read", calls[0].name)
        assertEquals("""{"title":"kanji"}""", calls[0].arguments)
    }

    @Test
    fun `accumulator keeps separate calls by index`() {
        val acc = ToolCallAccumulator()
        acc.add(
            listOf(
                ToolCallDelta(0, "c1", "memory_list", "{}"),
                ToolCallDelta(1, "c2", "memory_read", """{"title":"x"}"""),
            )
        )
        assertEquals(2, acc.build().size)
    }
}
