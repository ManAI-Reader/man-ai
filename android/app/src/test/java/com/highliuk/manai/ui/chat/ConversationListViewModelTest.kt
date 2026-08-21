package com.highliuk.manai.ui.chat

import app.cash.turbine.test
import com.highliuk.manai.domain.model.Conversation
import com.highliuk.manai.domain.repository.ChatRepository
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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

@OptIn(ExperimentalCoroutinesApi::class)
class ConversationListViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val chatRepository: ChatRepository = mockk(relaxed = true)

    private val grammarConversation = Conversation(
        id = 1L,
        title = "Explain grammar",
        createdAt = 100L,
        updatedAt = 200L,
    )

    private val vocabularyConversation = Conversation(
        id = 2L,
        title = "Vocabulary",
        createdAt = 300L,
        updatedAt = 400L,
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { chatRepository.observeConversations() } returns flowOf(emptyList())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() = ConversationListViewModel(chatRepository)

    @Test
    fun conversationsMirrorsRepositoryFlow() = runTest {
        every { chatRepository.observeConversations() } returns flowOf(
            listOf(grammarConversation, vocabularyConversation)
        )

        val viewModel = createViewModel()

        viewModel.conversations.test {
            assertEquals(emptyList<Conversation>(), awaitItem())
            assertEquals(listOf(grammarConversation, vocabularyConversation), awaitItem())
        }
    }

    @Test
    fun requestDeleteExposesPendingConversation() = runTest {
        val viewModel = createViewModel()

        viewModel.requestDelete(grammarConversation)

        assertEquals(grammarConversation, viewModel.pendingDelete.value)
    }

    @Test
    fun dismissDeleteClearsPendingConversation() = runTest {
        val viewModel = createViewModel()
        viewModel.requestDelete(grammarConversation)

        viewModel.dismissDelete()

        assertNull(viewModel.pendingDelete.value)
    }

    @Test
    fun confirmDeleteDeletesConversationAndClearsPending() = runTest {
        val viewModel = createViewModel()
        viewModel.requestDelete(grammarConversation)

        viewModel.confirmDelete()
        advanceUntilIdle()

        coVerify(exactly = 1) { chatRepository.deleteConversation(1L) }
        assertNull(viewModel.pendingDelete.value)
    }

    @Test
    fun confirmDeleteWithoutPendingConversationDoesNothing() = runTest {
        val viewModel = createViewModel()

        viewModel.confirmDelete()
        advanceUntilIdle()

        coVerify(exactly = 0) { chatRepository.deleteConversation(any()) }
    }
}
