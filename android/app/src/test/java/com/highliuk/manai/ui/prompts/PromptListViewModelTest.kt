package com.highliuk.manai.ui.prompts

import app.cash.turbine.test
import com.highliuk.manai.R
import com.highliuk.manai.domain.model.PromptTemplate
import com.highliuk.manai.domain.repository.PromptTemplateRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
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
    fun `requestNew opens dialog with blank template`() = runTest(testDispatcher) {
        val viewModel = createViewModel()

        viewModel.requestNew()

        assertEquals(
            PromptTemplate(id = 0L, name = "", template = ""),
            viewModel.editing.value,
        )
    }

    @Test
    fun `requestNew assigns next sortOrder after existing templates`() = runTest(testDispatcher) {
        templatesFlow.value = listOf(
            template.copy(id = 1L, sortOrder = 0),
            template.copy(id = 2L, sortOrder = 1),
            template.copy(id = 3L, sortOrder = 2),
            template.copy(id = 4L, sortOrder = 3),
        )
        val viewModel = createViewModel()
        backgroundScope.launch { viewModel.templates.collect() }
        advanceUntilIdle()

        viewModel.requestNew()

        assertEquals(4, viewModel.editing.value?.sortOrder)
    }

    @Test
    fun `requestEdit opens dialog with existing template`() = runTest(testDispatcher) {
        val viewModel = createViewModel()

        viewModel.requestEdit(template)

        assertEquals(template, viewModel.editing.value)
    }

    @Test
    fun `saveTemplate saves trimmed fields preserving id and sortOrder`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        viewModel.requestEdit(template)

        viewModel.saveTemplate("  New name  ", "  New body  ")
        advanceUntilIdle()

        coVerify {
            repository.save(
                PromptTemplate(id = 7L, name = "New name", template = "New body", sortOrder = 3),
            )
        }
        assertNull(viewModel.editing.value)
        assertNull(viewModel.editError.value)
    }

    @Test
    fun `saveTemplate rejects blank name keeping dialog open`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        viewModel.requestEdit(template)

        viewModel.saveTemplate("   ", "body")
        advanceUntilIdle()

        coVerify(exactly = 0) { repository.save(any()) }
        assertEquals(template, viewModel.editing.value)
        assertEquals(R.string.prompt_name_required, viewModel.editError.value)
    }

    @Test
    fun `saveTemplate rejects blank template keeping dialog open`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        viewModel.requestNew()

        viewModel.saveTemplate("name", "   ")
        advanceUntilIdle()

        coVerify(exactly = 0) { repository.save(any()) }
        assertEquals(
            PromptTemplate(id = 0L, name = "", template = ""),
            viewModel.editing.value,
        )
        assertEquals(R.string.prompt_name_required, viewModel.editError.value)
    }

    @Test
    fun `saveTemplate closes dialog synchronously and ignores rapid double invocation`() =
        runTest(testDispatcher) {
            val viewModel = createViewModel()
            viewModel.requestEdit(template)

            viewModel.saveTemplate("name", "body")
            assertNull(viewModel.editing.value)
            viewModel.saveTemplate("name", "body")
            advanceUntilIdle()

            coVerify(exactly = 1) { repository.save(any()) }
        }

    @Test
    fun `requestNew and requestEdit clear previous error`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        viewModel.requestEdit(template)
        viewModel.saveTemplate("", "")
        assertEquals(R.string.prompt_name_required, viewModel.editError.value)

        viewModel.requestNew()
        assertNull(viewModel.editError.value)

        viewModel.saveTemplate("", "")
        viewModel.requestEdit(template)
        assertNull(viewModel.editError.value)
    }

    @Test
    fun `dismissEdit clears editing and error`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        viewModel.requestEdit(template)
        viewModel.saveTemplate("", "")

        viewModel.dismissEdit()

        assertNull(viewModel.editing.value)
        assertNull(viewModel.editError.value)
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
