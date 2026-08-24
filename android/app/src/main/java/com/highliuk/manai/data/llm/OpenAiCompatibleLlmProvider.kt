package com.highliuk.manai.data.llm

import com.highliuk.manai.domain.llm.LlmEvent
import com.highliuk.manai.domain.llm.LlmFailure
import com.highliuk.manai.domain.llm.LlmMessage
import com.highliuk.manai.domain.llm.LlmProvider
import com.highliuk.manai.domain.llm.LlmToolSpec
import com.highliuk.manai.domain.logging.Logger
import com.highliuk.manai.domain.model.ReasoningLevel
import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.preparePost
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.readUTF8Line
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import java.io.IOException
import java.nio.channels.UnresolvedAddressException

class OpenAiCompatibleLlmProvider(
    private val httpClient: HttpClient,
    private val apiKeyProvider: suspend () -> String,
    private val baseUrlProvider: suspend () -> String,
    private val modelProvider: suspend () -> String,
    private val logger: Logger? = null,
) : LlmProvider {

    override fun chat(
        messages: List<LlmMessage>,
        tools: List<LlmToolSpec>,
        reasoning: ReasoningLevel,
    ): Flow<LlmEvent> = flow {
        val apiKey = apiKeyProvider()
        if (apiKey.isBlank()) {
            emit(LlmEvent.Failure(LlmFailure.Generic("LLM API key is not configured")))
            return@flow
        }
        try {
            stream(apiKey, StreamRequest(messages, tools, reasoning))
        } catch (e: CancellationException) {
            throw e
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            logger?.e(LOG_TAG, "LLM request failed", e)
            emit(LlmEvent.Failure(e.toLlmFailure()))
        }
    }

    /** DNS resolution failures with the CIO engine do not extend [IOException]. */
    private fun Exception.toLlmFailure(): LlmFailure = when (this) {
        is IOException, is UnresolvedAddressException -> LlmFailure.Network
        else -> LlmFailure.Generic(message)
    }

    private data class StreamRequest(
        val messages: List<LlmMessage>,
        val tools: List<LlmToolSpec>,
        val reasoning: ReasoningLevel,
    )

    private suspend fun FlowCollector<LlmEvent>.stream(apiKey: String, request: StreamRequest) {
        val url = baseUrlProvider().trimEnd('/') + "/chat/completions"
        val body = buildRequestBody(request).toString()
        httpClient.preparePost(url) {
            header(HttpHeaders.Authorization, "Bearer $apiKey")
            contentType(ContentType.Application.Json)
            setBody(body)
        }.execute { response ->
            if (!response.status.isSuccess()) {
                val errorBody = response.bodyAsText().take(MAX_ERROR_BODY)
                logger?.e(LOG_TAG, "LLM error ${response.status.value}: $errorBody")
                emit(LlmEvent.Failure(LlmFailure.Http(response.status.value)))
                return@execute
            }
            emitEventsFrom(response.bodyAsChannel())
        }
    }

    private suspend fun FlowCollector<LlmEvent>.emitEventsFrom(channel: ByteReadChannel) {
        val accumulator = ToolCallAccumulator()
        var finishReason: String? = null
        var line = channel.readUTF8Line()
        while (line != null) {
            OpenAiStreamParser.parseDataLine(line, logger)?.let { chunk ->
                chunk.contentDelta?.takeIf { it.isNotEmpty() }?.let { emit(LlmEvent.TextDelta(it)) }
                if (chunk.toolCallDeltas.isNotEmpty()) accumulator.add(chunk.toolCallDeltas)
                chunk.finishReason?.let { finishReason = it }
            }
            line = channel.readUTF8Line()
        }
        if (finishReason == FINISH_TOOL_CALLS && accumulator.hasCalls()) {
            emit(LlmEvent.ToolCalls(accumulator.build()))
        }
        emit(LlmEvent.Completed(finishReason))
    }

    private suspend fun buildRequestBody(request: StreamRequest): JsonObject {
        val model = modelProvider()
        return buildJsonObject {
            put("model", model)
            put("stream", true)
            put("max_tokens", MAX_COMPLETION_TOKENS)
            request.reasoning.toApiValue()?.let { put("reasoning_effort", it) }
            putJsonArray("messages") {
                request.messages.forEach { message -> add(message.toJson()) }
            }
            if (request.tools.isNotEmpty()) {
                putJsonArray("tools") {
                    request.tools.forEach { tool -> add(tool.toJson()) }
                }
            }
        }
    }

    /** Maps the level to the `reasoning_effort` value, or null when the parameter must be omitted. */
    private fun ReasoningLevel.toApiValue(): String? = when (this) {
        ReasoningLevel.DEFAULT -> null
        ReasoningLevel.OFF -> "none"
        ReasoningLevel.LOW -> "low"
        ReasoningLevel.MEDIUM -> "medium"
        ReasoningLevel.HIGH -> "high"
    }

    private fun LlmMessage.toJson(): JsonObject = buildJsonObject {
        put("role", role)
        content?.let { put("content", it) }
        toolCallId?.let { put("tool_call_id", it) }
        if (toolCalls.isNotEmpty()) {
            putJsonArray("tool_calls") {
                toolCalls.forEach { call ->
                    add(
                        buildJsonObject {
                            put("id", call.id)
                            put("type", "function")
                            putJsonObject("function") {
                                put("name", call.name)
                                put("arguments", call.arguments)
                            }
                        }
                    )
                }
            }
        }
    }

    private fun LlmToolSpec.toJson(): JsonObject = buildJsonObject {
        put("type", "function")
        putJsonObject("function") {
            put("name", name)
            put("description", description)
            put("parameters", Json.parseToJsonElement(parametersJsonSchema))
        }
    }

    private companion object {
        const val FINISH_TOOL_CALLS = "tool_calls"
        const val MAX_ERROR_BODY = 200
        const val LOG_TAG = "OpenAiCompatibleLlmProvider"

        /**
         * Explicit completion cap sent as the OpenAI-compatible `max_tokens`
         * field (the standard name Groq accepts). Without it some providers
         * apply a small default and end the stream with `finish_reason=length`,
         * silently truncating long tutor replies. 8192 is far above any answer
         * the app expects while still bounding runaway generations.
         */
        const val MAX_COMPLETION_TOKENS = 8192
    }
}
