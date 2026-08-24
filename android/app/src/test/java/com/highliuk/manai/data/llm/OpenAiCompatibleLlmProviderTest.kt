package com.highliuk.manai.data.llm

import app.cash.turbine.test
import com.highliuk.manai.domain.llm.LlmEvent
import com.highliuk.manai.domain.llm.LlmFailure
import com.highliuk.manai.domain.llm.LlmMessage
import com.highliuk.manai.domain.llm.LlmRequestConfig
import com.highliuk.manai.domain.llm.LlmToolSpec
import com.highliuk.manai.domain.logging.Logger
import com.highliuk.manai.domain.model.LlmVendor
import com.highliuk.manai.domain.model.ReasoningLevel
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.nio.channels.UnresolvedAddressException

class OpenAiCompatibleLlmProviderTest {

    private fun provider(
        engine: MockEngine,
        groqKey: String = "gsk_test",
        deepseekKey: String = "sk_test",
        logger: Logger? = null,
    ) = OpenAiCompatibleLlmProvider(
        httpClient = HttpClient(engine),
        apiKeyProvider = { vendor ->
            when (vendor) {
                LlmVendor.GROQ -> groqKey
                LlmVendor.DEEPSEEK -> deepseekKey
            }
        },
        logger = logger,
    )

    private fun config(
        vendor: LlmVendor = LlmVendor.GROQ,
        model: String = vendor.defaultModel,
        reasoning: ReasoningLevel = ReasoningLevel.DEFAULT,
    ) = LlmRequestConfig(vendor = vendor, model = model, reasoning = reasoning)

    private fun sse(vararg lines: String): String = lines.joinToString("\n\n") + "\n\n"

    private fun sseEngine(vararg lines: String) = MockEngine {
        respond(
            content = sse(*lines),
            status = HttpStatusCode.OK,
            headers = headersOf(HttpHeaders.ContentType, "text/event-stream"),
        )
    }

    private fun doneEngine() = sseEngine("data: [DONE]")

    private val userMessage = listOf(LlmMessage(LlmMessage.ROLE_USER, "hi"))

    @Test
    fun `streams text deltas then completes`() = runTest {
        val engine = sseEngine(
            """data: {"choices":[{"delta":{"content":"Hel"},"finish_reason":null}]}""",
            """data: {"choices":[{"delta":{"content":"lo"},"finish_reason":null}]}""",
            """data: {"choices":[{"delta":{},"finish_reason":"stop"}]}""",
            "data: [DONE]",
        )
        provider(engine).chat(userMessage, emptyList(), config()).test {
            assertEquals(LlmEvent.TextDelta("Hel"), awaitItem())
            assertEquals(LlmEvent.TextDelta("lo"), awaitItem())
            assertEquals(LlmEvent.Completed("stop"), awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `emits accumulated tool calls when finish reason is tool_calls`() = runTest {
        val engine = sseEngine(
            """data: {"choices":[{"delta":{"tool_calls":[{"index":0,"id":"c1",""" +
                """"function":{"name":"memory_read","arguments":"{\"ti"}}]},"finish_reason":null}]}""",
            """data: {"choices":[{"delta":{"tool_calls":[{"index":0,""" +
                """"function":{"arguments":"tle\":\"kanji\"}"}}]},"finish_reason":null}]}""",
            """data: {"choices":[{"delta":{},"finish_reason":"tool_calls"}]}""",
            "data: [DONE]",
        )
        provider(engine).chat(userMessage, emptyList(), config()).test {
            val toolCalls = awaitItem() as LlmEvent.ToolCalls
            assertEquals("memory_read", toolCalls.calls[0].name)
            assertEquals("""{"title":"kanji"}""", toolCalls.calls[0].arguments)
            assertEquals(LlmEvent.Completed("tool_calls"), awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `emits typed http failure on error status without leaking the body`() = runTest {
        val engine = MockEngine { respond("secret provider body", HttpStatusCode.Unauthorized) }
        provider(engine).chat(userMessage, emptyList(), config()).test {
            assertEquals(LlmEvent.Failure(LlmFailure.Http(401)), awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `http error body is logged but never surfaced in the event`() = runTest {
        val logger = mockk<Logger>(relaxed = true)
        val engine = MockEngine { respond("secret provider body", HttpStatusCode.BadGateway) }
        provider(engine, logger = logger)
            .chat(userMessage, emptyList(), config())
            .test {
                assertEquals(LlmEvent.Failure(LlmFailure.Http(502)), awaitItem())
                awaitComplete()
            }
        verify { logger.e("OpenAiCompatibleLlmProvider", match { it.contains("secret provider body") }) }
    }

    @Test
    fun `logs malformed stream chunk through injected logger`() = runTest {
        val logger = mockk<Logger>(relaxed = true)
        val engine = sseEngine(
            "data: {broken json",
            """data: {"choices":[{"delta":{},"finish_reason":"stop"}]}""",
            "data: [DONE]",
        )
        provider(engine, logger = logger).chat(userMessage, emptyList(), config()).test {
            assertEquals(LlmEvent.Completed("stop"), awaitItem())
            awaitComplete()
        }
        verify { logger.e("OpenAiStreamParser", match { it.contains("{broken json") }) }
    }

    @Test
    fun `emits network failure and logs the exception when engine throws io exception`() = runTest {
        val logger = mockk<Logger>(relaxed = true)
        val engine = MockEngine { throw IOException("dns timeout") }
        provider(engine, logger = logger)
            .chat(userMessage, emptyList(), config())
            .test {
                assertEquals(LlmEvent.Failure(LlmFailure.Network), awaitItem())
                awaitComplete()
            }
        verify { logger.e("OpenAiCompatibleLlmProvider", any(), any<IOException>()) }
    }

    @Test
    fun `emits network failure on unresolved address`() = runTest {
        val engine = MockEngine { throw UnresolvedAddressException() }
        provider(engine).chat(userMessage, emptyList(), config()).test {
            assertEquals(LlmEvent.Failure(LlmFailure.Network), awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `emits generic failure when engine throws a non-io exception`() = runTest {
        val engine = MockEngine { throw IllegalStateException("weird") }
        provider(engine).chat(userMessage, emptyList(), config()).test {
            assertEquals(LlmEvent.Failure(LlmFailure.Generic("weird")), awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `emits generic failure when the vendor api key is blank`() = runTest {
        val engine = MockEngine { respond("never called") }
        provider(engine, groqKey = "")
            .chat(userMessage, emptyList(), config(vendor = LlmVendor.GROQ))
            .test {
                assertTrue((awaitItem() as LlmEvent.Failure).failure is LlmFailure.Generic)
                awaitComplete()
            }
    }

    @Test
    fun `a blank key for one vendor does not block the other vendor`() = runTest {
        provider(doneEngine(), deepseekKey = "")
            .chat(userMessage, emptyList(), config(vendor = LlmVendor.GROQ))
            .test {
                assertEquals(LlmEvent.Completed(), awaitItem())
                awaitComplete()
            }
    }

    @Test
    fun `completed event carries the length finish reason`() = runTest {
        val engine = sseEngine(
            """data: {"choices":[{"delta":{"content":"cut"},"finish_reason":null}]}""",
            """data: {"choices":[{"delta":{},"finish_reason":"length"}]}""",
            "data: [DONE]",
        )
        provider(engine).chat(userMessage, emptyList(), config()).test {
            assertEquals(LlmEvent.TextDelta("cut"), awaitItem())
            assertEquals(LlmEvent.Completed("length"), awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `groq requests hit the groq base url with the groq key`() = runTest {
        val engine = doneEngine()
        provider(engine, groqKey = "gsk_groq")
            .chat(userMessage, emptyList(), config(vendor = LlmVendor.GROQ))
            .test {
                assertEquals(LlmEvent.Completed(), awaitItem())
                awaitComplete()
            }
        val request = engine.requestHistory.single()
        assertEquals("https://api.groq.com/openai/v1/chat/completions", request.url.toString())
        assertEquals("Bearer gsk_groq", request.headers[HttpHeaders.Authorization])
    }

    @Test
    fun `deepseek requests hit the deepseek base url with the deepseek key`() = runTest {
        val engine = doneEngine()
        provider(engine, deepseekKey = "sk_deep")
            .chat(userMessage, emptyList(), config(vendor = LlmVendor.DEEPSEEK))
            .test {
                assertEquals(LlmEvent.Completed(), awaitItem())
                awaitComplete()
            }
        val request = engine.requestHistory.single()
        assertEquals("https://api.deepseek.com/v1/chat/completions", request.url.toString())
        assertEquals("Bearer sk_deep", request.headers[HttpHeaders.Authorization])
    }

    @Test
    fun `request body contains model stream flag messages and tools`() = runTest {
        val engine = doneEngine()
        val tools = listOf(
            LlmToolSpec(
                name = "memory_read",
                description = "Read a memory note",
                parametersJsonSchema = """{"type":"object","properties":{"title":{"type":"string"}}}""",
            )
        )
        provider(engine)
            .chat(userMessage, tools, config(model = "openai/gpt-oss-120b"))
            .test {
                assertEquals(LlmEvent.Completed(), awaitItem())
                awaitComplete()
            }
        val body = engine.requestHistory.single().body.toByteArray().decodeToString()
        assertTrue(body.contains(""""model":"openai/gpt-oss-120b""""))
        assertTrue(body.contains(""""stream":true"""))
        assertTrue(body.contains(""""role":"user""""))
        assertTrue(body.contains(""""name":"memory_read""""))
    }

    @Test
    fun `request body omits the tools field entirely when the tool list is empty`() = runTest {
        val engine = doneEngine()
        provider(engine).chat(userMessage, emptyList(), config()).test {
            assertEquals(LlmEvent.Completed(), awaitItem())
            awaitComplete()
        }
        val body = engine.requestHistory.single().body.toByteArray().decodeToString()
        assertFalse(body.contains("\"tools\""))
    }

    private suspend fun requestBodyFor(
        vendor: LlmVendor,
        reasoning: ReasoningLevel,
    ): String {
        val engine = doneEngine()
        provider(engine)
            .chat(userMessage, emptyList(), config(vendor = vendor, reasoning = reasoning))
            .test {
                assertEquals(LlmEvent.Completed(), awaitItem())
                awaitComplete()
            }
        return engine.requestHistory.single().body.toByteArray().decodeToString()
    }

    @Test
    fun `groq body never contains max_tokens for any reasoning level`() = runTest {
        ReasoningLevel.entries.forEach { level ->
            assertFalse(
                "max_tokens leaked for $level",
                requestBodyFor(LlmVendor.GROQ, level).contains("\"max_tokens\""),
            )
        }
    }

    @Test
    fun `groq body sends reasoning_effort only for explicit effort levels`() = runTest {
        assertTrue(
            requestBodyFor(LlmVendor.GROQ, ReasoningLevel.LOW)
                .contains(""""reasoning_effort":"low""""),
        )
        assertTrue(
            requestBodyFor(LlmVendor.GROQ, ReasoningLevel.MEDIUM)
                .contains(""""reasoning_effort":"medium""""),
        )
        assertTrue(
            requestBodyFor(LlmVendor.GROQ, ReasoningLevel.HIGH)
                .contains(""""reasoning_effort":"high""""),
        )
    }

    @Test
    fun `groq body omits reasoning_effort for default and off levels`() = runTest {
        assertFalse(requestBodyFor(LlmVendor.GROQ, ReasoningLevel.DEFAULT).contains("reasoning_effort"))
        assertFalse(requestBodyFor(LlmVendor.GROQ, ReasoningLevel.OFF).contains("reasoning_effort"))
    }

    @Test
    fun `deepseek body always caps max_tokens at 8192`() = runTest {
        ReasoningLevel.entries.forEach { level ->
            assertTrue(
                "max_tokens missing for $level",
                requestBodyFor(LlmVendor.DEEPSEEK, level).contains(""""max_tokens":8192"""),
            )
        }
    }

    @Test
    fun `deepseek body maps off to reasoning_effort none`() = runTest {
        assertTrue(
            requestBodyFor(LlmVendor.DEEPSEEK, ReasoningLevel.OFF)
                .contains(""""reasoning_effort":"none""""),
        )
    }

    @Test
    fun `deepseek body sends explicit effort levels`() = runTest {
        assertTrue(
            requestBodyFor(LlmVendor.DEEPSEEK, ReasoningLevel.LOW)
                .contains(""""reasoning_effort":"low""""),
        )
        assertTrue(
            requestBodyFor(LlmVendor.DEEPSEEK, ReasoningLevel.MEDIUM)
                .contains(""""reasoning_effort":"medium""""),
        )
        assertTrue(
            requestBodyFor(LlmVendor.DEEPSEEK, ReasoningLevel.HIGH)
                .contains(""""reasoning_effort":"high""""),
        )
    }

    @Test
    fun `deepseek body omits reasoning_effort for the default level`() = runTest {
        assertFalse(
            requestBodyFor(LlmVendor.DEEPSEEK, ReasoningLevel.DEFAULT).contains("reasoning_effort"),
        )
    }
}
