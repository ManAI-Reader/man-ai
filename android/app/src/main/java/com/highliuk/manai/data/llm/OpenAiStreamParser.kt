package com.highliuk.manai.data.llm

import com.highliuk.manai.domain.llm.LlmToolCall
import com.highliuk.manai.domain.logging.Logger
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

@Serializable
internal data class StreamChunk(val choices: List<StreamChoice> = emptyList())

@Serializable
internal data class StreamChoice(
    val delta: StreamDelta = StreamDelta(),
    @SerialName("finish_reason") val finishReason: String? = null,
)

@Serializable
internal data class StreamDelta(
    val content: String? = null,
    @SerialName("tool_calls") val toolCalls: List<RawToolCallDelta> = emptyList(),
)

@Serializable
internal data class RawToolCallDelta(
    val index: Int = 0,
    val id: String? = null,
    val function: RawFunctionDelta = RawFunctionDelta(),
)

@Serializable
internal data class RawFunctionDelta(
    val name: String? = null,
    val arguments: String? = null,
)

data class ToolCallDelta(
    val index: Int,
    val id: String?,
    val name: String?,
    val argumentsFragment: String?,
)

data class ParsedChunk(
    val contentDelta: String?,
    val toolCallDeltas: List<ToolCallDelta>,
    val finishReason: String?,
)

object OpenAiStreamParser {
    private const val DATA_PREFIX = "data:"
    private const val DONE = "[DONE]"
    private const val LOG_TAG = "OpenAiStreamParser"
    private const val MAX_LOGGED_PAYLOAD = 200
    private val json = Json { ignoreUnknownKeys = true }

    fun parseDataLine(line: String, logger: Logger? = null): ParsedChunk? =
        extractPayload(line)
            ?.let { decode(it, logger) }
            ?.choices
            ?.firstOrNull()
            ?.toParsedChunk()

    private fun extractPayload(line: String): String? {
        if (!line.startsWith(DATA_PREFIX)) return null
        val payload = line.removePrefix(DATA_PREFIX).trim()
        return payload.takeUnless { it.isEmpty() || it == DONE }
    }

    private fun decode(payload: String, logger: Logger?): StreamChunk? = try {
        json.decodeFromString<StreamChunk>(payload)
    } catch (_: SerializationException) {
        logMalformed(payload, logger)
        null
    } catch (_: IllegalArgumentException) {
        logMalformed(payload, logger)
        null
    }

    private fun logMalformed(payload: String, logger: Logger?) {
        logger?.e(LOG_TAG, "Malformed stream chunk: ${payload.take(MAX_LOGGED_PAYLOAD)}")
    }

    private fun StreamChoice.toParsedChunk(): ParsedChunk = ParsedChunk(
        contentDelta = delta.content,
        toolCallDeltas = delta.toolCalls.map {
            ToolCallDelta(it.index, it.id, it.function.name, it.function.arguments)
        },
        finishReason = finishReason,
    )
}

class ToolCallAccumulator {
    private data class Partial(
        var id: String = "",
        var name: String = "",
        val args: StringBuilder = StringBuilder(),
    )

    private val partials = sortedMapOf<Int, Partial>()

    fun add(deltas: List<ToolCallDelta>) {
        for (delta in deltas) {
            val partial = partials.getOrPut(delta.index) { Partial() }
            delta.id?.let { partial.id = it }
            delta.name?.let { partial.name = it }
            delta.argumentsFragment?.let { partial.args.append(it) }
        }
    }

    fun hasCalls(): Boolean = partials.isNotEmpty()

    fun build(): List<LlmToolCall> = partials.values.map {
        LlmToolCall(id = it.id, name = it.name, arguments = it.args.toString())
    }
}
