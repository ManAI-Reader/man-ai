package com.highliuk.manai.domain.chat

import com.highliuk.manai.domain.llm.LlmToolCall
import com.highliuk.manai.domain.llm.LlmToolSpec
import com.highliuk.manai.domain.repository.MemoryRepository
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject

class MemoryToolExecutor @Inject constructor(
    private val memoryRepository: MemoryRepository,
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun execute(call: LlmToolCall): String = try {
        when (call.name) {
            TOOL_MEMORY_LIST -> buildJsonArray {
                memoryRepository.listTitles().forEach { add(it) }
            }.toString()
            TOOL_MEMORY_READ -> {
                val title = argument(call.arguments, "title")
                memoryRepository.read(title) ?: "No memory page titled '$title'."
            }
            TOOL_MEMORY_WRITE -> {
                memoryRepository.write(
                    title = argument(call.arguments, "title"),
                    content = argument(call.arguments, "content"),
                )
                "Saved."
            }
            else -> "Unknown tool: ${call.name}"
        }
    } catch (_: IllegalArgumentException) {
        "Invalid arguments for ${call.name}: ${call.arguments}"
    } catch (_: SerializationException) {
        "Invalid arguments for ${call.name}: ${call.arguments}"
    }

    private fun argument(rawJson: String, key: String): String =
        json.parseToJsonElement(rawJson).jsonObject[key]?.jsonPrimitive?.contentOrNull
            ?: throw IllegalArgumentException("missing $key")

    companion object {
        const val TOOL_MEMORY_LIST = "memory_list"
        const val TOOL_MEMORY_READ = "memory_read"
        const val TOOL_MEMORY_WRITE = "memory_write"

        val SPECS: List<LlmToolSpec> = listOf(
            LlmToolSpec(
                name = TOOL_MEMORY_LIST,
                description = "List the titles of all pages in your persistent memory wiki about this learner.",
                parametersJsonSchema = """{"type":"object","properties":{},"required":[]}""",
            ),
            LlmToolSpec(
                name = TOOL_MEMORY_READ,
                description = "Read the content of a memory wiki page by title.",
                parametersJsonSchema =
                    """{"type":"object","properties":{"title":{"type":"string"}},"required":["title"]}""",
            ),
            LlmToolSpec(
                name = TOOL_MEMORY_WRITE,
                description = "Create or overwrite a memory wiki page. Use short, topical pages.",
                parametersJsonSchema =
                    """{"type":"object","properties":{"title":{"type":"string"},"content":{"type":"string"}}""" +
                        ""","required":["title","content"]}""",
            ),
        )
    }
}
