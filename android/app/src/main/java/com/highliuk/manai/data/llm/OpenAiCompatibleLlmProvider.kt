package com.highliuk.manai.data.llm

import com.highliuk.manai.domain.llm.LlmEvent
import com.highliuk.manai.domain.llm.LlmFailure
import com.highliuk.manai.domain.llm.LlmMessage
import com.highliuk.manai.domain.llm.LlmProvider
import com.highliuk.manai.domain.llm.LlmRequestConfig
import com.highliuk.manai.domain.llm.LlmToolSpec
import com.highliuk.manai.domain.logging.Logger
import com.highliuk.manai.domain.model.LlmVendor
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
    private val apiKeyProvider: suspend (LlmVendor) -> String,
    private val logger: Logger? = null,
) : LlmProvider {

    override fun chat(
        messages: List<LlmMessage>,
        tools: List<LlmToolSpec>,
        config: LlmRequestConfig,
    ): Flow<LlmEvent> = flow {
        val apiKey = apiKeyProvider(config.vendor)
        if (apiKey.isBlank()) {
            emit(LlmEvent.Failure(LlmFailure.Generic("LLM API key is not configured")))
            return@flow
        }
        try {
            stream(apiKey, StreamRequest(messages, tools, config))
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
        val config: LlmRequestConfig,
    )

    private suspend fun FlowCollector<LlmEvent>.stream(apiKey: String, request: StreamRequest) {
        val url = request.config.vendor.baseUrl + "/chat/completions"
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

    private fun buildRequestBody(request: StreamRequest): JsonObject = buildJsonObject {
        put("model", request.config.model)
        put("stream", true)
        if (request.config.vendor == LlmVendor.DEEPSEEK) {
            // DeepSeek's thinking tokens consume the completion budget, so an
            // explicit generous cap keeps long reasoning from being cut short
            // by the (smaller) server default.
            put("max_tokens", DEEPSEEK_MAX_TOKENS)
        }
        request.config.reasoningApiValue()?.let { put("reasoning_effort", it) }
        putJsonArray("messages") {
            request.messages.forEach { message -> add(message.toJson()) }
        }
        if (request.tools.isNotEmpty()) {
            putJsonArray("tools") {
                request.tools.forEach { tool -> add(tool.toJson()) }
            }
        }
    }

    /**
     * Maps the reasoning level to the vendor's `reasoning_effort` value, or
     * null when the parameter must be omitted.
     *
     * Groq only accepts low/medium/high and rejects `"none"` (and any other
     * value) with a 400 on gpt-oss models, so both [ReasoningLevel.DEFAULT]
     * and [ReasoningLevel.OFF] are sent as an omission. DeepSeek accepts
     * `"none"` to disable thinking, so only [ReasoningLevel.DEFAULT] is
     * omitted there.
     */
    private fun LlmRequestConfig.reasoningApiValue(): String? = when (reasoning) {
        ReasoningLevel.DEFAULT -> null
        ReasoningLevel.OFF -> if (vendor == LlmVendor.DEEPSEEK) "none" else null
        ReasoningLevel.LOW -> "low"
        ReasoningLevel.MEDIUM -> "medium"
        ReasoningLevel.HIGH -> "high"
    }

    /**
     * Groq is intentionally absent from any `max_tokens` handling: on its
     * free tier the per-minute token limit counts prompt plus `max_tokens`,
     * so sending the parameter triggers spurious 413s.
     */
    private val LlmVendor.baseUrl: String
        get() = when (this) {
            LlmVendor.GROQ -> "https://api.groq.com/openai/v1"
            LlmVendor.DEEPSEEK -> "https://api.deepseek.com/v1"
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

        /** See [buildRequestBody]: thinking counts against this completion budget. */
        const val DEEPSEEK_MAX_TOKENS = 8192
    }
}
