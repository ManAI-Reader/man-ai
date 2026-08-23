package com.highliuk.manai.domain.usecase

import app.cash.turbine.test
import com.highliuk.manai.domain.chat.MemoryToolExecutor
import com.highliuk.manai.domain.chat.SystemPromptBuilder
import com.highliuk.manai.domain.llm.LlmEvent
import com.highliuk.manai.domain.llm.LlmMessage
import com.highliuk.manai.domain.llm.LlmProvider
import com.highliuk.manai.domain.llm.LlmToolCall
import com.highliuk.manai.domain.llm.LlmToolSpec
import com.highliuk.manai.domain.model.ChatMessage
import com.highliuk.manai.domain.model.ChatRole
import com.highliuk.manai.domain.model.Conversation
import com.highliuk.manai.domain.model.ReasoningLevel
import com.highliuk.manai.domain.model.TargetLanguage
import com.highliuk.manai.domain.repository.ChatRepository
import com.highliuk.manai.domain.repository.MemoryRepository
import com.highliuk.manai.domain.repository.UserPreferencesRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GenerateChatReplyUseCaseTest {

    private val chatRepository = mockk<ChatRepository>(relaxed = true) {
        every { observeConversation(any()) } returns flowOf(null)
    }
    private val memoryRepository = mockk<MemoryRepository>()
    private val userPreferencesRepository = mockk<UserPreferencesRepository> {
        coEvery { translationTargetLang } returns flowOf(TargetLanguage.IT)
    }

    private class FakeLlmProvider(
        private val responses: List<List<LlmEvent>>,
    ) : LlmProvider {
        val recordedMessages = mutableListOf<List<LlmMessage>>()
        val recordedReasonings = mutableListOf<ReasoningLevel>()
        val recordedTools = mutableListOf<List<LlmToolSpec>>()
        private var callIndex = 0

        override fun chat(
            messages: List<LlmMessage>,
            tools: List<LlmToolSpec>,
            reasoning: ReasoningLevel,
        ): Flow<LlmEvent> {
            recordedMessages += messages.toList()
            recordedReasonings += reasoning
            recordedTools += tools.toList()
            val events = responses[minOf(callIndex, responses.lastIndex)]
            callIndex++
            return events.asFlow()
        }
    }

    private fun useCase(provider: LlmProvider) = GenerateChatReplyUseCase(
        chatRepository = chatRepository,
        llmProvider = provider,
        memoryToolExecutor = MemoryToolExecutor(memoryRepository),
        userPreferencesRepository = userPreferencesRepository,
    )

    private fun userMessage(content: String) = ChatMessage(
        id = 1L,
        conversationId = 42L,
        role = ChatRole.USER,
        content = content,
        timestamp = 0L,
    )

    /** A history past the first turn: the full agent must be active. */
    private fun followUpHistory() = listOf(
        ChatMessage(1L, 42L, ChatRole.USER, "Q1", 0L),
        ChatMessage(2L, 42L, ChatRole.ASSISTANT, "A1", 1L),
        ChatMessage(3L, 42L, ChatRole.USER, "Hello", 2L),
    )

    @Test
    fun `text-only round streams accumulated deltas and persists one assistant message`() = runTest {
        coEvery { chatRepository.getMessages(42L) } returns listOf(userMessage("Hello"))
        val provider = FakeLlmProvider(
            listOf(
                listOf(LlmEvent.TextDelta("A"), LlmEvent.TextDelta("B"), LlmEvent.Completed),
            ),
        )

        useCase(provider).invoke(42L).test {
            assertEquals(ChatGenerationEvent.Delta("A"), awaitItem())
            assertEquals(ChatGenerationEvent.Delta("AB"), awaitItem())
            assertEquals(ChatGenerationEvent.Done, awaitItem())
            awaitComplete()
        }

        coVerify(exactly = 1) { chatRepository.appendMessage(42L, ChatRole.ASSISTANT, "AB") }
    }

    @Test
    fun `tool round executes tool and feeds result back to provider`() = runTest {
        coEvery { chatRepository.getMessages(42L) } returns followUpHistory()
        coEvery { memoryRepository.listTitles() } returns listOf("Level")
        val toolCall = LlmToolCall(id = "call-1", name = "memory_list", arguments = "{}")
        val provider = FakeLlmProvider(
            listOf(
                listOf(LlmEvent.ToolCalls(listOf(toolCall)), LlmEvent.Completed),
                listOf(LlmEvent.TextDelta("Hi"), LlmEvent.Completed),
            ),
        )

        useCase(provider).invoke(42L).test {
            assertEquals(ChatGenerationEvent.Delta("Hi"), awaitItem())
            assertEquals(ChatGenerationEvent.Done, awaitItem())
            awaitComplete()
        }

        coVerify(exactly = 1) { memoryRepository.listTitles() }
        assertEquals(2, provider.recordedMessages.size)
        val secondCallHistory = provider.recordedMessages[1]
        val assistantToolCallMessage = secondCallHistory.single { it.toolCalls.isNotEmpty() }
        assertEquals(LlmMessage.ROLE_ASSISTANT, assistantToolCallMessage.role)
        assertEquals(listOf(toolCall), assistantToolCallMessage.toolCalls)
        val toolMessage = secondCallHistory.single { it.role == LlmMessage.ROLE_TOOL }
        assertEquals("""["Level"]""", toolMessage.content)
        assertEquals("call-1", toolMessage.toolCallId)
        coVerify(exactly = 1) { chatRepository.appendMessage(42L, ChatRole.ASSISTANT, "Hi") }
    }

    @Test
    fun `each tool round history message carries only that round's text`() = runTest {
        coEvery { chatRepository.getMessages(42L) } returns followUpHistory()
        coEvery { memoryRepository.listTitles() } returns emptyList()
        coEvery { memoryRepository.read("Level") } returns "N4"
        val callOne = LlmToolCall(id = "call-1", name = "memory_list", arguments = "{}")
        val callTwo = LlmToolCall(id = "call-2", name = "memory_read", arguments = """{"title":"Level"}""")
        val provider = FakeLlmProvider(
            listOf(
                listOf(LlmEvent.TextDelta("One"), LlmEvent.ToolCalls(listOf(callOne)), LlmEvent.Completed),
                listOf(LlmEvent.TextDelta("Two"), LlmEvent.ToolCalls(listOf(callTwo)), LlmEvent.Completed),
                listOf(LlmEvent.TextDelta("!"), LlmEvent.Completed),
            ),
        )

        useCase(provider).invoke(42L).test {
            assertEquals(ChatGenerationEvent.Delta("One"), awaitItem())
            assertEquals(ChatGenerationEvent.Delta("OneTwo"), awaitItem())
            assertEquals(ChatGenerationEvent.Delta("OneTwo!"), awaitItem())
            assertEquals(ChatGenerationEvent.Done, awaitItem())
            awaitComplete()
        }

        val roundTwoHistory = provider.recordedMessages[1]
        assertEquals("One", roundTwoHistory.single { it.toolCalls.isNotEmpty() }.content)

        val roundThreeHistory = provider.recordedMessages[2]
        val assistantToolMessages = roundThreeHistory.filter { it.toolCalls.isNotEmpty() }
        assertEquals(2, assistantToolMessages.size)
        assertEquals("One", assistantToolMessages[0].content)
        assertEquals("Two", assistantToolMessages[1].content)

        coVerify(exactly = 1) { chatRepository.appendMessage(42L, ChatRole.ASSISTANT, "OneTwo!") }
    }

    @Test
    fun `tool calls split across multiple events are all executed`() = runTest {
        coEvery { chatRepository.getMessages(42L) } returns followUpHistory()
        coEvery { memoryRepository.listTitles() } returns emptyList()
        coEvery { memoryRepository.read("Level") } returns "N4"
        val callA = LlmToolCall(id = "call-a", name = "memory_list", arguments = "{}")
        val callB = LlmToolCall(id = "call-b", name = "memory_read", arguments = """{"title":"Level"}""")
        val provider = FakeLlmProvider(
            listOf(
                listOf(
                    LlmEvent.ToolCalls(listOf(callA)),
                    LlmEvent.ToolCalls(listOf(callB)),
                    LlmEvent.Completed,
                ),
                listOf(LlmEvent.TextDelta("ok"), LlmEvent.Completed),
            ),
        )

        useCase(provider).invoke(42L).test {
            assertEquals(ChatGenerationEvent.Delta("ok"), awaitItem())
            assertEquals(ChatGenerationEvent.Done, awaitItem())
            awaitComplete()
        }

        val secondCallHistory = provider.recordedMessages[1]
        assertEquals(
            listOf(callA, callB),
            secondCallHistory.single { it.toolCalls.isNotEmpty() }.toolCalls,
        )
        assertEquals(
            listOf("call-a", "call-b"),
            secondCallHistory.filter { it.role == LlmMessage.ROLE_TOOL }.map { it.toolCallId },
        )
    }

    @Test
    fun `provider failure emits error and persists nothing`() = runTest {
        coEvery { chatRepository.getMessages(42L) } returns listOf(userMessage("Hello"))
        val provider = FakeLlmProvider(
            listOf(listOf(LlmEvent.TextDelta("partial"), LlmEvent.Failure("boom"))),
        )

        useCase(provider).invoke(42L).test {
            assertEquals(ChatGenerationEvent.Delta("partial"), awaitItem())
            assertEquals(ChatGenerationEvent.Error("boom"), awaitItem())
            awaitComplete()
        }

        coVerify(exactly = 0) { chatRepository.appendMessage(any(), any(), any()) }
    }

    @Test
    fun `runaway tool calls stop with error after max rounds`() = runTest {
        coEvery { chatRepository.getMessages(42L) } returns followUpHistory()
        coEvery { memoryRepository.listTitles() } returns emptyList()
        val toolCall = LlmToolCall(id = "call-1", name = "memory_list", arguments = "{}")
        val provider = FakeLlmProvider(
            listOf(listOf(LlmEvent.ToolCalls(listOf(toolCall)), LlmEvent.Completed)),
        )

        useCase(provider).invoke(42L).test {
            val event = awaitItem()
            assertTrue(event is ChatGenerationEvent.Error)
            awaitComplete()
        }

        assertEquals(5, provider.recordedMessages.size)
        coVerify(exactly = 0) { chatRepository.appendMessage(any(), any(), any()) }
    }

    @Test
    fun `conversation reasoning level is passed to the provider on every round`() = runTest {
        coEvery { chatRepository.getMessages(42L) } returns followUpHistory()
        coEvery { memoryRepository.listTitles() } returns emptyList()
        every { chatRepository.observeConversation(42L) } returns flowOf(
            Conversation(
                id = 42L,
                title = "Chat",
                createdAt = 0L,
                updatedAt = 0L,
                reasoningLevel = ReasoningLevel.HIGH,
            )
        )
        val toolCall = LlmToolCall(id = "call-1", name = "memory_list", arguments = "{}")
        val provider = FakeLlmProvider(
            listOf(
                listOf(LlmEvent.ToolCalls(listOf(toolCall)), LlmEvent.Completed),
                listOf(LlmEvent.TextDelta("Hi"), LlmEvent.Completed),
            ),
        )

        useCase(provider).invoke(42L).test {
            assertEquals(ChatGenerationEvent.Delta("Hi"), awaitItem())
            assertEquals(ChatGenerationEvent.Done, awaitItem())
            awaitComplete()
        }

        assertEquals(listOf(ReasoningLevel.HIGH, ReasoningLevel.HIGH), provider.recordedReasonings)
    }

    @Test
    fun `missing conversation falls back to DEFAULT reasoning`() = runTest {
        coEvery { chatRepository.getMessages(42L) } returns listOf(userMessage("Hello"))
        val provider = FakeLlmProvider(
            listOf(listOf(LlmEvent.TextDelta("ok"), LlmEvent.Completed)),
        )

        useCase(provider).invoke(42L).test {
            assertEquals(ChatGenerationEvent.Delta("ok"), awaitItem())
            assertEquals(ChatGenerationEvent.Done, awaitItem())
            awaitComplete()
        }

        assertEquals(listOf(ReasoningLevel.DEFAULT), provider.recordedReasonings)
    }

    @Test
    fun `history starts with system prompt then persisted messages in order`() = runTest {
        coEvery { chatRepository.getMessages(42L) } returns listOf(
            ChatMessage(1L, 42L, ChatRole.USER, "Q1", 0L),
            ChatMessage(2L, 42L, ChatRole.ASSISTANT, "A1", 1L),
            ChatMessage(3L, 42L, ChatRole.USER, "Q2", 2L),
        )
        val provider = FakeLlmProvider(
            listOf(listOf(LlmEvent.TextDelta("ok"), LlmEvent.Completed)),
        )

        useCase(provider).invoke(42L).test {
            assertEquals(ChatGenerationEvent.Delta("ok"), awaitItem())
            assertEquals(ChatGenerationEvent.Done, awaitItem())
            awaitComplete()
        }

        val history = provider.recordedMessages.single()
        assertEquals(4, history.size)
        assertEquals(LlmMessage.ROLE_SYSTEM, history[0].role)
        assertEquals(SystemPromptBuilder.build(TargetLanguage.IT), history[0].content)
        assertEquals(LlmMessage.ROLE_USER, history[1].role)
        assertEquals("Q1", history[1].content)
        assertEquals(LlmMessage.ROLE_ASSISTANT, history[2].role)
        assertEquals("A1", history[2].content)
        assertEquals(LlmMessage.ROLE_USER, history[3].role)
        assertEquals("Q2", history[3].content)
    }

    @Test
    fun `first turn is vanilla - no tools and minimal system prompt`() = runTest {
        coEvery { chatRepository.getMessages(42L) } returns listOf(userMessage("rendered template"))
        val provider = FakeLlmProvider(
            listOf(listOf(LlmEvent.TextDelta("ok"), LlmEvent.Completed)),
        )

        useCase(provider).invoke(42L).test {
            assertEquals(ChatGenerationEvent.Delta("ok"), awaitItem())
            assertEquals(ChatGenerationEvent.Done, awaitItem())
            awaitComplete()
        }

        assertEquals(listOf(emptyList<LlmToolSpec>()), provider.recordedTools)
        val history = provider.recordedMessages.single()
        assertEquals(LlmMessage.ROLE_SYSTEM, history[0].role)
        assertEquals(SystemPromptBuilder.buildVanilla(TargetLanguage.IT), history[0].content)
        assertEquals("rendered template", history[1].content)
    }

    @Test
    fun `follow-up turn advertises the memory tools`() = runTest {
        coEvery { chatRepository.getMessages(42L) } returns followUpHistory()
        val provider = FakeLlmProvider(
            listOf(listOf(LlmEvent.TextDelta("ok"), LlmEvent.Completed)),
        )

        useCase(provider).invoke(42L).test {
            assertEquals(ChatGenerationEvent.Delta("ok"), awaitItem())
            assertEquals(ChatGenerationEvent.Done, awaitItem())
            awaitComplete()
        }

        assertEquals(listOf(MemoryToolExecutor.SPECS), provider.recordedTools)
    }

    @Test
    fun `reasoning level is still applied on the vanilla first turn`() = runTest {
        coEvery { chatRepository.getMessages(42L) } returns listOf(userMessage("rendered template"))
        every { chatRepository.observeConversation(42L) } returns flowOf(
            Conversation(
                id = 42L,
                title = "Chat",
                createdAt = 0L,
                updatedAt = 0L,
                reasoningLevel = ReasoningLevel.HIGH,
            )
        )
        val provider = FakeLlmProvider(
            listOf(listOf(LlmEvent.TextDelta("ok"), LlmEvent.Completed)),
        )

        useCase(provider).invoke(42L).test {
            assertEquals(ChatGenerationEvent.Delta("ok"), awaitItem())
            assertEquals(ChatGenerationEvent.Done, awaitItem())
            awaitComplete()
        }

        assertEquals(listOf(ReasoningLevel.HIGH), provider.recordedReasonings)
        assertEquals(listOf(emptyList<LlmToolSpec>()), provider.recordedTools)
    }

    @Test
    fun `two user messages without an assistant reply use the full agent`() = runTest {
        coEvery { chatRepository.getMessages(42L) } returns listOf(
            ChatMessage(1L, 42L, ChatRole.USER, "Q1", 0L),
            ChatMessage(2L, 42L, ChatRole.USER, "Q2", 1L),
        )
        val provider = FakeLlmProvider(
            listOf(listOf(LlmEvent.TextDelta("ok"), LlmEvent.Completed)),
        )

        useCase(provider).invoke(42L).test {
            assertEquals(ChatGenerationEvent.Delta("ok"), awaitItem())
            assertEquals(ChatGenerationEvent.Done, awaitItem())
            awaitComplete()
        }

        assertEquals(listOf(MemoryToolExecutor.SPECS), provider.recordedTools)
        assertEquals(
            SystemPromptBuilder.build(TargetLanguage.IT),
            provider.recordedMessages.single()[0].content,
        )
    }
}
