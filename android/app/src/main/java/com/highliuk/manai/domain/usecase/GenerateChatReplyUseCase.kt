package com.highliuk.manai.domain.usecase

import com.highliuk.manai.domain.chat.MemoryToolExecutor
import com.highliuk.manai.domain.chat.SystemPromptBuilder
import com.highliuk.manai.domain.llm.LlmEvent
import com.highliuk.manai.domain.llm.LlmMessage
import com.highliuk.manai.domain.llm.LlmProvider
import com.highliuk.manai.domain.llm.LlmToolCall
import com.highliuk.manai.domain.model.ChatRole
import com.highliuk.manai.domain.model.ReasoningLevel
import com.highliuk.manai.domain.model.TargetLanguage
import com.highliuk.manai.domain.repository.ChatRepository
import com.highliuk.manai.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

sealed class ChatGenerationEvent {
    data class Delta(val accumulatedText: String) : ChatGenerationEvent()
    data object Done : ChatGenerationEvent()
    data class Error(val message: String) : ChatGenerationEvent()
}

/**
 * Streams the assistant reply for a conversation whose last persisted message is from the user,
 * running an agent loop that lets the model call memory tools between LLM rounds.
 */
class GenerateChatReplyUseCase @Inject constructor(
    private val chatRepository: ChatRepository,
    private val llmProvider: LlmProvider,
    private val memoryToolExecutor: MemoryToolExecutor,
    private val userPreferencesRepository: UserPreferencesRepository,
) {
    /**
     * Streams [ChatGenerationEvent]s for the given conversation.
     *
     * LLM provider failures are surfaced as [ChatGenerationEvent.Error], but exceptions thrown
     * by the repositories or the tool executor propagate through the flow: collectors
     * (e.g. ViewModels) must apply `.catch` to handle them.
     */
    operator fun invoke(conversationId: Long): Flow<ChatGenerationEvent> = flow {
        val targetLang = userPreferencesRepository.translationTargetLang.first()
        val reasoning = chatRepository.observeConversation(conversationId).first()
            ?.reasoningLevel ?: ReasoningLevel.DEFAULT
        val history = buildHistory(conversationId, targetLang).toMutableList()
        val fullText = StringBuilder()
        var rounds = 0
        var finished = false
        while (!finished && rounds < MAX_TOOL_ROUNDS) {
            rounds++
            finished = runRound(RoundInput(conversationId, reasoning, history), fullText)
        }
        if (!finished) {
            emit(ChatGenerationEvent.Error("Tool-call limit reached"))
        }
    }

    /**
     * Runs one LLM round: streams deltas, then either terminates the flow (failure or final
     * answer) or appends tool results to [history] for the next round.
     *
     * @return true when the generation is finished (Done or Error emitted).
     */
    /** Immutable per-generation inputs shared by every round. */
    private data class RoundInput(
        val conversationId: Long,
        val reasoning: ReasoningLevel,
        val history: MutableList<LlmMessage>,
    )

    private suspend fun FlowCollector<ChatGenerationEvent>.runRound(
        input: RoundInput,
        fullText: StringBuilder,
    ): Boolean {
        val (conversationId, reasoning, history) = input
        var pendingToolCalls: List<LlmToolCall> = emptyList()
        var failure: String? = null
        val roundText = StringBuilder()
        llmProvider.chat(history, MemoryToolExecutor.SPECS, reasoning).collect { event ->
            when (event) {
                is LlmEvent.TextDelta -> {
                    roundText.append(event.text)
                    fullText.append(event.text)
                    emit(ChatGenerationEvent.Delta(fullText.toString()))
                }
                is LlmEvent.ToolCalls -> pendingToolCalls = pendingToolCalls + event.calls
                is LlmEvent.Failure -> failure = event.message
                LlmEvent.Completed -> Unit
            }
        }
        val failureMessage = failure
        return when {
            failureMessage != null -> {
                emit(ChatGenerationEvent.Error(failureMessage))
                true
            }
            pendingToolCalls.isEmpty() -> {
                chatRepository.appendMessage(conversationId, ChatRole.ASSISTANT, fullText.toString())
                emit(ChatGenerationEvent.Done)
                true
            }
            else -> {
                history += LlmMessage(
                    role = LlmMessage.ROLE_ASSISTANT,
                    content = roundText.toString().ifEmpty { null },
                    toolCalls = pendingToolCalls,
                )
                pendingToolCalls.forEach { call ->
                    history += LlmMessage(
                        role = LlmMessage.ROLE_TOOL,
                        content = memoryToolExecutor.execute(call),
                        toolCallId = call.id,
                    )
                }
                false
            }
        }
    }

    private suspend fun buildHistory(
        conversationId: Long,
        targetLang: TargetLanguage,
    ): List<LlmMessage> {
        val system = LlmMessage(LlmMessage.ROLE_SYSTEM, SystemPromptBuilder.build(targetLang))
        val persisted = chatRepository.getMessages(conversationId).map { message ->
            LlmMessage(
                role = when (message.role) {
                    ChatRole.USER -> LlmMessage.ROLE_USER
                    ChatRole.ASSISTANT -> LlmMessage.ROLE_ASSISTANT
                },
                content = message.content,
            )
        }
        return listOf(system) + persisted
    }

    private companion object {
        const val MAX_TOOL_ROUNDS = 5
    }
}
