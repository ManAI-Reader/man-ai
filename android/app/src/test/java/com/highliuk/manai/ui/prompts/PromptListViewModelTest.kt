package com.highliuk.manai.ui.prompts

import app.cash.turbine.test
import com.highliuk.manai.domain.model.PromptTemplate
import com.highliuk.manai.domain.repository.PromptTemplateRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
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
class PromptListViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val templatesFlow = MutableStateFlow<List<PromptTemplate>>(emptyList())
    private val repository = mockk<PromptTemplateRepository>(relaxed = true) {
        coEvery { observeTemplates() } returns templatesFlow
    }

    private val template = PromptTemplate(
        id = 7L,
        name = "Explain",
        template = "Explain {text}",
        sortOrder = 3,
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() = PromptListViewModel(repository)

    @Test
    fun `templates emits values from repository`() = runTest(testDispatcher) {
        val viewModel = createViewModel()

        viewModel.templates.test {
            assertEquals(emptyList<PromptTemplate>(), awaitItem())
            templatesFlow.value = listOf(template)
            assertEquals(listOf(template), awaitItem())
        }
    }

    @Test
    fun `requestDelete sets pendingDelete`() = runTest(testDispatcher) {
        val viewModel = createViewModel()

        viewModel.requestDelete(template)

        assertEquals(template, viewModel.pendingDelete.value)
    }

    @Test
    fun `confirmDelete deletes via repository and clears pending`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        viewModel.requestDelete(template)

        viewModel.confirmDelete()
        advanceUntilIdle()

        coVerify { repository.delete(7L) }
        assertNull(viewModel.pendingDelete.value)
    }

    @Test
    fun `dismissDelete clears pending without deleting`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        viewModel.requestDelete(template)

        viewModel.dismissDelete()
        advanceUntilIdle()

        coVerify(exactly = 0) { repository.delete(any()) }
        assertNull(viewModel.pendingDelete.value)
    }
}
