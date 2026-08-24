package com.highliuk.manai.ui.prompts

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.highliuk.manai.R
import com.highliuk.manai.domain.model.PromptTemplate
import com.highliuk.manai.domain.model.ReasoningLevel
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
class PromptEditViewModelTest {

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

    private fun createViewModel(id: Long) = PromptEditViewModel(
        SavedStateHandle(mapOf("id" to id)),
        repository,
    )

    @Test
    fun `new template loads blank with next sortOrder`() = runTest(testDispatcher) {
        templatesFlow.value = listOf(
            template.copy(id = 1L, sortOrder = 0),
            template.copy(id = 2L, sortOrder = 4),
        )
        val viewModel = createViewModel(id = -1L)
        advanceUntilIdle()

        assertEquals(
            PromptTemplate(id = 0L, name = "", template = "", sortOrder = 5),
            viewModel.template.value,
        )
    }

    @Test
    fun `existing template is loaded by id`() = runTest(testDispatcher) {
        templatesFlow.value = listOf(template)
        val viewModel = createViewModel(id = 7L)
        advanceUntilIdle()

        assertEquals(template, viewModel.template.value)
    }

    @Test
    fun `save trims fields preserving id and sortOrder and emits saved`() =
        runTest(testDispatcher) {
            templatesFlow.value = listOf(template)
            val viewModel = createViewModel(id = 7L)
            advanceUntilIdle()

            viewModel.saved.test {
                viewModel.save("  New name  ", "  New body  ", ReasoningLevel.HIGH)
                awaitItem()
            }

            coVerify {
                repository.save(
                    PromptTemplate(
                        id = 7L,
                        name = "New name",
                        template = "New body",
                        sortOrder = 3,
                        reasoningLevel = ReasoningLevel.HIGH,
                    ),
                )
            }
            assertNull(viewModel.editError.value)
        }

    @Test
    fun `save rejects blank name with validation error`() = runTest(testDispatcher) {
        templatesFlow.value = listOf(template)
        val viewModel = createViewModel(id = 7L)
        advanceUntilIdle()

        viewModel.save("   ", "body", ReasoningLevel.DEFAULT)
        advanceUntilIdle()

        coVerify(exactly = 0) { repository.save(any()) }
        assertEquals(R.string.prompt_name_required, viewModel.editError.value)
    }

    @Test
    fun `save rejects blank template with validation error`() = runTest(testDispatcher) {
        val viewModel = createViewModel(id = -1L)
        advanceUntilIdle()

        viewModel.save("name", "   ", ReasoningLevel.DEFAULT)
        advanceUntilIdle()

        coVerify(exactly = 0) { repository.save(any()) }
        assertEquals(R.string.prompt_name_required, viewModel.editError.value)
    }

    @Test
    fun `successful save after a validation error clears it`() = runTest(testDispatcher) {
        val viewModel = createViewModel(id = -1L)
        advanceUntilIdle()

        viewModel.save("", "", ReasoningLevel.DEFAULT)
        assertEquals(R.string.prompt_name_required, viewModel.editError.value)

        viewModel.save("name", "body", ReasoningLevel.DEFAULT)
        advanceUntilIdle()

        assertNull(viewModel.editError.value)
    }

    @Test
    fun `rapid double save persists only once`() = runTest(testDispatcher) {
        templatesFlow.value = listOf(template)
        val viewModel = createViewModel(id = 7L)
        advanceUntilIdle()

        viewModel.save("name", "body", ReasoningLevel.DEFAULT)
        viewModel.save("name", "body", ReasoningLevel.DEFAULT)
        advanceUntilIdle()

        coVerify(exactly = 1) { repository.save(any()) }
    }
}
