package com.highliuk.manai.domain.llm

import com.highliuk.manai.domain.model.ReasoningLevel
import kotlinx.coroutines.flow.Flow

data class LlmMessage(
    val role: String,
    val content: String? = null,
    val toolCallId: String? = null,
    val toolCalls: List<LlmToolCall> = emptyList(),
) {
    companion object {
        const val ROLE_SYSTEM = "system"
        const val ROLE_USER = "user"
        const val ROLE_ASSISTANT = "assistant"
        const val ROLE_TOOL = "tool"
    }
}

data class LlmToolCall(
    val id: String,
    val name: String,
    val arguments: String,
)

data class LlmToolSpec(
    val name: String,
    val description: String,
    val parametersJsonSchema: String,
)

/**
 * Streaming events emitted by [LlmProvider.chat].
 *
 * Terminal contract: the flow ends with exactly one of [Failure] or [Completed].
 * [Failure] is terminal and is never followed by [Completed].
 */
sealed class LlmEvent {
    data class TextDelta(val text: String) : LlmEvent()
    data class ToolCalls(val calls: List<LlmToolCall>) : LlmEvent()
    data class Failure(val message: String) : LlmEvent()
    data object Completed : LlmEvent()
}

interface LlmProvider {
    /**
     * Streams the model reply for [messages], advertising [tools] to the model.
     *
     * @param reasoning reasoning effort to request from the model. Providers
     * must not send any reasoning parameter when the level is
     * [ReasoningLevel.DEFAULT]; [ReasoningLevel.OFF] maps to the provider
     * value `"none"`.
     */
    fun chat(
        messages: List<LlmMessage>,
        tools: List<LlmToolSpec>,
        reasoning: ReasoningLevel = ReasoningLevel.DEFAULT,
    ): Flow<LlmEvent>
}
