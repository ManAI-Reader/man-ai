package com.highliuk.manai.ui.chat

import app.cash.turbine.test
import com.highliuk.manai.domain.model.Conversation
import com.highliuk.manai.domain.repository.ChatRepository
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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
    fun openSearchActivatesSearchMode() = runTest {
        val viewModel = createViewModel()

        viewModel.openSearch()

        assertTrue(viewModel.isSearchActive.value)
    }

    @Test
    fun closeSearchDeactivatesSearchModeAndClearsQuery() = runTest {
        val viewModel = createViewModel()
        viewModel.openSearch()
        viewModel.onSearchQueryChange("grammar")

        viewModel.closeSearch()

        assertFalse(viewModel.isSearchActive.value)
        assertEquals("", viewModel.searchQuery.value)
    }

    @Test
    fun onSearchQueryChangeUpdatesQuery() = runTest {
        val viewModel = createViewModel()

        viewModel.onSearchQueryChange("grammar")

        assertEquals("grammar", viewModel.searchQuery.value)
    }

    @Test
    fun blankSearchQueryShowsFullConversationList() = runTest {
        every { chatRepository.observeConversations() } returns flowOf(
            listOf(grammarConversation, vocabularyConversation)
        )

        val viewModel = createViewModel()
        viewModel.openSearch()
        viewModel.onSearchQueryChange("   ")

        viewModel.conversations.test {
            skipItems(1)
            assertEquals(listOf(grammarConversation, vocabularyConversation), awaitItem())
        }
        verify(exactly = 0) { chatRepository.searchConversations(any()) }
    }

    @Test
    fun activeSearchWithQueryEmitsSearchResults() = runTest {
        every { chatRepository.observeConversations() } returns flowOf(
            listOf(grammarConversation, vocabularyConversation)
        )
        every { chatRepository.searchConversations("grammar") } returns flowOf(
            listOf(grammarConversation)
        )

        val viewModel = createViewModel()
        viewModel.openSearch()
        viewModel.onSearchQueryChange("grammar")

        viewModel.conversations.test {
            skipItems(1)
            assertEquals(listOf(grammarConversation), awaitItem())
        }
    }

    @Test
    fun queryWithoutActiveSearchShowsFullConversationList() = runTest {
        every { chatRepository.observeConversations() } returns flowOf(
            listOf(grammarConversation, vocabularyConversation)
        )

        val viewModel = createViewModel()
        viewModel.onSearchQueryChange("grammar")

        viewModel.conversations.test {
            skipItems(1)
            assertEquals(listOf(grammarConversation, vocabularyConversation), awaitItem())
        }
        verify(exactly = 0) { chatRepository.searchConversations(any()) }
    }

    @Test
    fun closeSearchRestoresFullConversationList() = runTest {
        every { chatRepository.observeConversations() } returns flowOf(
            listOf(grammarConversation, vocabularyConversation)
        )
        every { chatRepository.searchConversations("grammar") } returns flowOf(
            listOf(grammarConversation)
        )

        val viewModel = createViewModel()
        viewModel.openSearch()
        viewModel.onSearchQueryChange("grammar")

        viewModel.conversations.test {
            skipItems(1)
            assertEquals(listOf(grammarConversation), awaitItem())

            viewModel.closeSearch()

            assertEquals(listOf(grammarConversation, vocabularyConversation), awaitItem())
        }
        assertEquals("", viewModel.searchQuery.value)
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
