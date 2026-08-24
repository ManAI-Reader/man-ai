package com.highliuk.manai.data.llm

import app.cash.turbine.test
import com.highliuk.manai.domain.llm.LlmEvent
import com.highliuk.manai.domain.llm.LlmFailure
import com.highliuk.manai.domain.llm.LlmMessage
import com.highliuk.manai.domain.llm.LlmToolSpec
import com.highliuk.manai.domain.logging.Logger
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
        apiKey: String = "gsk_test",
        logger: Logger? = null,
    ) = OpenAiCompatibleLlmProvider(
        httpClient = HttpClient(engine),
        apiKeyProvider = { apiKey },
        baseUrlProvider = { "https://api.groq.com/openai/v1" },
        modelProvider = { "llama-3.3-70b-versatile" },
        logger = logger,
    )

    private fun sse(vararg lines: String): String = lines.joinToString("\n\n") + "\n\n"

    @Test
    fun `streams text deltas then completes`() = runTest {
        val engine = MockEngine {
            respond(
                content = sse(
                    """data: {"choices":[{"delta":{"content":"Hel"},"finish_reason":null}]}""",
                    """data: {"choices":[{"delta":{"content":"lo"},"finish_reason":null}]}""",
                    """data: {"choices":[{"delta":{},"finish_reason":"stop"}]}""",
                    "data: [DONE]",
                ),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "text/event-stream"),
            )
        }
        provider(engine).chat(listOf(LlmMessage(LlmMessage.ROLE_USER, "hi")), emptyList()).test {
            assertEquals(LlmEvent.TextDelta("Hel"), awaitItem())
            assertEquals(LlmEvent.TextDelta("lo"), awaitItem())
            assertEquals(LlmEvent.Completed("stop"), awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `emits accumulated tool calls when finish reason is tool_calls`() = runTest {
        val engine = MockEngine {
            respond(
                content = sse(
                    """data: {"choices":[{"delta":{"tool_calls":[{"index":0,"id":"c1",""" +
                        """"function":{"name":"memory_read","arguments":"{\"ti"}}]},"finish_reason":null}]}""",
                    """data: {"choices":[{"delta":{"tool_calls":[{"index":0,""" +
                        """"function":{"arguments":"tle\":\"kanji\"}"}}]},"finish_reason":null}]}""",
                    """data: {"choices":[{"delta":{},"finish_reason":"tool_calls"}]}""",
                    "data: [DONE]",
                ),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "text/event-stream"),
            )
        }
        provider(engine).chat(listOf(LlmMessage(LlmMessage.ROLE_USER, "hi")), emptyList()).test {
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
        provider(engine).chat(listOf(LlmMessage(LlmMessage.ROLE_USER, "hi")), emptyList()).test {
            assertEquals(LlmEvent.Failure(LlmFailure.Http(401)), awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `http error body is logged but never surfaced in the event`() = runTest {
        val logger = mockk<Logger>(relaxed = true)
        val engine = MockEngine { respond("secret provider body", HttpStatusCode.BadGateway) }
        provider(engine, logger = logger)
            .chat(listOf(LlmMessage(LlmMessage.ROLE_USER, "hi")), emptyList())
            .test {
                assertEquals(LlmEvent.Failure(LlmFailure.Http(502)), awaitItem())
                awaitComplete()
            }
        verify { logger.e("OpenAiCompatibleLlmProvider", match { it.contains("secret provider body") }) }
    }

    @Test
    fun `logs malformed stream chunk through injected logger`() = runTest {
        val logger = mockk<Logger>(relaxed = true)
        val engine = MockEngine {
            respond(
                content = sse(
                    "data: {broken json",
                    """data: {"choices":[{"delta":{},"finish_reason":"stop"}]}""",
                    "data: [DONE]",
                ),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "text/event-stream"),
            )
        }
        provider(engine, logger = logger).chat(listOf(LlmMessage(LlmMessage.ROLE_USER, "hi")), emptyList()).test {
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
            .chat(listOf(LlmMessage(LlmMessage.ROLE_USER, "hi")), emptyList())
            .test {
                assertEquals(LlmEvent.Failure(LlmFailure.Network), awaitItem())
                awaitComplete()
            }
        verify { logger.e("OpenAiCompatibleLlmProvider", any(), any<IOException>()) }
    }

    @Test
    fun `emits network failure on unresolved address`() = runTest {
        val engine = MockEngine { throw UnresolvedAddressException() }
        provider(engine).chat(listOf(LlmMessage(LlmMessage.ROLE_USER, "hi")), emptyList()).test {
            assertEquals(LlmEvent.Failure(LlmFailure.Network), awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `emits generic failure when engine throws a non-io exception`() = runTest {
        val engine = MockEngine { throw IllegalStateException("weird") }
        provider(engine).chat(listOf(LlmMessage(LlmMessage.ROLE_USER, "hi")), emptyList()).test {
            assertEquals(LlmEvent.Failure(LlmFailure.Generic("weird")), awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `emits generic failure when api key is blank`() = runTest {
        val engine = MockEngine { respond("never called") }
        provider(engine, apiKey = "").chat(listOf(LlmMessage(LlmMessage.ROLE_USER, "hi")), emptyList()).test {
            assertTrue((awaitItem() as LlmEvent.Failure).failure is LlmFailure.Generic)
            awaitComplete()
        }
    }

    @Test
    fun `completed event carries the length finish reason`() = runTest {
        val engine = MockEngine {
            respond(
                content = sse(
                    """data: {"choices":[{"delta":{"content":"cut"},"finish_reason":null}]}""",
                    """data: {"choices":[{"delta":{},"finish_reason":"length"}]}""",
                    "data: [DONE]",
                ),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "text/event-stream"),
            )
        }
        provider(engine).chat(listOf(LlmMessage(LlmMessage.ROLE_USER, "hi")), emptyList()).test {
            assertEquals(LlmEvent.TextDelta("cut"), awaitItem())
            assertEquals(LlmEvent.Completed("length"), awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `request body contains model stream flag messages and tools`() = runTest {
        val engine = MockEngine {
            respond(
                content = sse("data: [DONE]"),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "text/event-stream"),
            )
        }
        val tools = listOf(
            LlmToolSpec(
                name = "memory_read",
                description = "Read a memory note",
                parametersJsonSchema = """{"type":"object","properties":{"title":{"type":"string"}}}""",
            )
        )
        provider(engine).chat(listOf(LlmMessage(LlmMessage.ROLE_USER, "hi")), tools).test {
            assertEquals(LlmEvent.Completed(), awaitItem())
            awaitComplete()
        }
        val body = engine.requestHistory.single().body.toByteArray().decodeToString()
        assertTrue(body.contains(""""model":"llama-3.3-70b-versatile""""))
        assertTrue(body.contains(""""stream":true"""))
        assertTrue(body.contains(""""role":"user""""))
        assertTrue(body.contains(""""name":"memory_read""""))
    }

    @Test
    fun `request body caps the completion size with an explicit max_tokens`() = runTest {
        val engine = MockEngine {
            respond(
                content = sse("data: [DONE]"),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "text/event-stream"),
            )
        }
        provider(engine).chat(listOf(LlmMessage(LlmMessage.ROLE_USER, "hi")), emptyList()).test {
            assertEquals(LlmEvent.Completed(), awaitItem())
            awaitComplete()
        }
        val body = engine.requestHistory.single().body.toByteArray().decodeToString()
        assertTrue(body.contains(""""max_tokens":8192"""))
    }

    @Test
    fun `request body omits the tools field entirely when the tool list is empty`() = runTest {
        val engine = MockEngine {
            respond(
                content = sse("data: [DONE]"),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "text/event-stream"),
            )
        }
        provider(engine).chat(listOf(LlmMessage(LlmMessage.ROLE_USER, "hi")), emptyList()).test {
            assertEquals(LlmEvent.Completed(), awaitItem())
            awaitComplete()
        }
        val body = engine.requestHistory.single().body.toByteArray().decodeToString()
        assertFalse(body.contains("\"tools\""))
    }

    private suspend fun requestBodyFor(reasoning: ReasoningLevel): String {
        val engine = MockEngine {
            respond(
                content = sse("data: [DONE]"),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "text/event-stream"),
            )
        }
        provider(engine)
            .chat(listOf(LlmMessage(LlmMessage.ROLE_USER, "hi")), emptyList(), reasoning)
            .test {
                assertEquals(LlmEvent.Completed(), awaitItem())
                awaitComplete()
            }
        return engine.requestHistory.single().body.toByteArray().decodeToString()
    }

    @Test
    fun `request body contains reasoning_effort for every non-default level`() = runTest {
        assertTrue(requestBodyFor(ReasoningLevel.OFF).contains(""""reasoning_effort":"none""""))
        assertTrue(requestBodyFor(ReasoningLevel.LOW).contains(""""reasoning_effort":"low""""))
        assertTrue(requestBodyFor(ReasoningLevel.MEDIUM).contains(""""reasoning_effort":"medium""""))
        assertTrue(requestBodyFor(ReasoningLevel.HIGH).contains(""""reasoning_effort":"high""""))
    }

    @Test
    fun `request body omits reasoning_effort for default level`() = runTest {
        assertFalse(requestBodyFor(ReasoningLevel.DEFAULT).contains("reasoning_effort"))
    }
}
