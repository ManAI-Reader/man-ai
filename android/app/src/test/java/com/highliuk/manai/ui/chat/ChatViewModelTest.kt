package com.highliuk.manai.ui.chat

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.highliuk.manai.domain.model.ChatMessage
import com.highliuk.manai.domain.model.ChatRole
import com.highliuk.manai.domain.repository.ChatRepository
import com.highliuk.manai.domain.usecase.ChatGenerationEvent
import com.highliuk.manai.domain.usecase.GenerateChatReplyUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

private class MessagelessException : RuntimeException()

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val chatRepository: ChatRepository = mockk(relaxed = true)
    private val generateChatReply: GenerateChatReplyUseCase = mockk()

    private fun userMessage(content: String = "question") = ChatMessage(
        id = 1L, conversationId = 42L, role = ChatRole.USER, content = content, timestamp = 0L
    )

    private fun assistantMessage(content: String = "answer") = ChatMessage(
        id = 2L, conversationId = 42L, role = ChatRole.ASSISTANT, content = content, timestamp = 0L
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { chatRepository.observeConversation(42L) } returns flowOf(null)
        every { chatRepository.observeMessages(42L) } returns flowOf(emptyList())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() = ChatViewModel(
        SavedStateHandle(mapOf("conversationId" to 42L)),
        chatRepository,
        generateChatReply,
    )

    @Test
    fun autoGeneratesOnInitWhenLastPersistedMessageIsUser() = runTest {
        coEvery { chatRepository.getMessages(42L) } returns listOf(userMessage())
        every { generateChatReply(42L) } returns flowOf(
            ChatGenerationEvent.Delta("Hel"),
            ChatGenerationEvent.Delta("Hello"),
            ChatGenerationEvent.Done,
        )

        val viewModel = createViewModel()

        viewModel.streamingText.test {
            assertNull(awaitItem())
            assertEquals("Hel", awaitItem())
            assertEquals("Hello", awaitItem())
            assertNull(awaitItem())
        }
        verify(exactly = 1) { generateChatReply(42L) }
    }

    @Test
    fun doesNotAutoGenerateWhenLastMessageIsAssistant() = runTest {
        coEvery { chatRepository.getMessages(42L) } returns listOf(userMessage(), assistantMessage())

        createViewModel()
        advanceUntilIdle()

        verify(exactly = 0) { generateChatReply(any()) }
    }

    @Test
    fun sendMessagePersistsTrimmedTextThenStreams() = runTest {
        coEvery { chatRepository.getMessages(42L) } returns emptyList()
        every { generateChatReply(42L) } returns flowOf(
            ChatGenerationEvent.Delta("Sure"),
            ChatGenerationEvent.Done,
        )

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.streamingText.test {
            assertNull(awaitItem())
            viewModel.sendMessage("  what does this mean?  ")
            assertEquals("Sure", awaitItem())
            assertNull(awaitItem())
        }
        coVerify { chatRepository.appendMessage(42L, ChatRole.USER, "what does this mean?") }
    }

    @Test
    fun blankSendMessageIsIgnored() = runTest {
        coEvery { chatRepository.getMessages(42L) } returns emptyList()

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.sendMessage("   ")
        advanceUntilIdle()

        coVerify(exactly = 0) { chatRepository.appendMessage(any(), any(), any()) }
        verify(exactly = 0) { generateChatReply(any()) }
    }

    @Test
    fun secondSendMessageWhileGeneratingIsIgnored() = runTest {
        coEvery { chatRepository.getMessages(42L) } returns emptyList()
        every { generateChatReply(42L) } returns flow {
            emit(ChatGenerationEvent.Delta("thinking"))
            kotlinx.coroutines.awaitCancellation()
        }

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.sendMessage("first")
        advanceUntilIdle()
        viewModel.sendMessage("second")
        advanceUntilIdle()

        coVerify(exactly = 1) { chatRepository.appendMessage(42L, ChatRole.USER, any()) }
        verify(exactly = 1) { generateChatReply(42L) }
    }

    @Test
    fun sendMessageDuringInitAutoGenerationBeforeFirstDeltaIsIgnored() = runTest {
        coEvery { chatRepository.getMessages(42L) } returns listOf(userMessage())
        every { generateChatReply(42L) } returns flow {
            kotlinx.coroutines.awaitCancellation()
        }

        val viewModel = createViewModel()
        // Init auto-generation is accepted but has not emitted any delta yet.
        viewModel.sendMessage("second question")
        advanceUntilIdle()

        coVerify(exactly = 0) { chatRepository.appendMessage(any(), any(), any()) }
    }

    @Test
    fun twoRapidSendMessagesAppendOnlyOneMessage() = runTest {
        coEvery { chatRepository.getMessages(42L) } returns emptyList()
        every { generateChatReply(42L) } returns flow {
            emit(ChatGenerationEvent.Delta("thinking"))
            kotlinx.coroutines.awaitCancellation()
        }

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.sendMessage("first")
        viewModel.sendMessage("second")
        advanceUntilIdle()

        coVerify(exactly = 1) { chatRepository.appendMessage(42L, ChatRole.USER, any()) }
        verify(exactly = 1) { generateChatReply(42L) }
    }

    @Test
    fun isGeneratingTransitionsFalseTrueFalseAroundGeneration() = runTest {
        coEvery { chatRepository.getMessages(42L) } returns emptyList()
        every { generateChatReply(42L) } returns flowOf(
            ChatGenerationEvent.Delta("Hi"),
            ChatGenerationEvent.Done,
        )

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.isGenerating.test {
            assertEquals(false, awaitItem())
            viewModel.sendMessage("hello")
            assertEquals(true, awaitItem())
            assertEquals(false, awaitItem())
        }
    }

    @Test
    fun appendMessageFailureSetsErrorAndDoesNotCrash() = runTest {
        coEvery { chatRepository.getMessages(42L) } returns emptyList()
        coEvery {
            chatRepository.appendMessage(any(), any(), any())
        } throws IllegalStateException("db full")

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.sendMessage("hello")
        advanceUntilIdle()

        assertEquals("db full", viewModel.error.value)
        assertEquals(false, viewModel.isGenerating.value)
        verify(exactly = 0) { generateChatReply(any()) }
    }

    @Test
    fun errorEventSetsErrorAndRetryRegeneratesWhenLastMessageIsUser() = runTest {
        coEvery { chatRepository.getMessages(42L) } returns listOf(userMessage())
        every { generateChatReply(42L) } returns flowOf(
            ChatGenerationEvent.Delta("par"),
            ChatGenerationEvent.Error("provider down"),
        )

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals("provider down", viewModel.error.value)
        assertNull(viewModel.streamingText.value)

        every { generateChatReply(42L) } returns flowOf(
            ChatGenerationEvent.Delta("ok"),
            ChatGenerationEvent.Done,
        )
        viewModel.retry()
        advanceUntilIdle()

        assertNull(viewModel.error.value)
        verify(exactly = 2) { generateChatReply(42L) }
    }

    @Test
    fun retryDoesNotRegenerateWhenLastMessageIsAssistant() = runTest {
        coEvery { chatRepository.getMessages(42L) } returns listOf(userMessage(), assistantMessage())

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.retry()
        advanceUntilIdle()

        verify(exactly = 0) { generateChatReply(any()) }
    }

    @Test
    fun thrownExceptionFromFlowIsCaughtAndSetsError() = runTest {
        coEvery { chatRepository.getMessages(42L) } returns listOf(userMessage())
        every { generateChatReply(42L) } returns flow {
            emit(ChatGenerationEvent.Delta("par"))
            error("repository exploded")
        }

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals("repository exploded", viewModel.error.value)
        assertNull(viewModel.streamingText.value)
    }

    @Test
    fun thrownExceptionWithoutMessageStoresBlankErrorForLocalizedFallback() = runTest {
        coEvery { chatRepository.getMessages(42L) } returns listOf(userMessage())
        every { generateChatReply(42L) } returns flow<ChatGenerationEvent> {
            throw MessagelessException()
        }

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals("", viewModel.error.value)
    }
}
