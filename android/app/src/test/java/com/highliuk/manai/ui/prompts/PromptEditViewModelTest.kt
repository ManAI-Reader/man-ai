package com.highliuk.manai.ui.prompts

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.highliuk.manai.R
import com.highliuk.manai.domain.model.LlmVendor
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
                viewModel.save(
                    "  New name  ",
                    "  New body  ",
                    ReasoningLevel.HIGH,
                    LlmVendor.GROQ,
                    LlmVendor.GROQ.defaultModel,
                )
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

        viewModel.save("   ", "body", ReasoningLevel.DEFAULT, LlmVendor.GROQ, "m")
        advanceUntilIdle()

        coVerify(exactly = 0) { repository.save(any()) }
        assertEquals(R.string.prompt_name_required, viewModel.editError.value)
    }

    @Test
    fun `save rejects blank template with validation error`() = runTest(testDispatcher) {
        val viewModel = createViewModel(id = -1L)
        advanceUntilIdle()

        viewModel.save("name", "   ", ReasoningLevel.DEFAULT, LlmVendor.GROQ, "m")
        advanceUntilIdle()

        coVerify(exactly = 0) { repository.save(any()) }
        assertEquals(R.string.prompt_name_required, viewModel.editError.value)
    }

    @Test
    fun `successful save after a validation error clears it`() = runTest(testDispatcher) {
        val viewModel = createViewModel(id = -1L)
        advanceUntilIdle()

        viewModel.save("", "", ReasoningLevel.DEFAULT, LlmVendor.GROQ, "m")
        assertEquals(R.string.prompt_name_required, viewModel.editError.value)

        viewModel.save("name", "body", ReasoningLevel.DEFAULT, LlmVendor.GROQ, "m")
        advanceUntilIdle()

        assertNull(viewModel.editError.value)
    }

    @Test
    fun `rapid double save persists only once`() = runTest(testDispatcher) {
        templatesFlow.value = listOf(template)
        val viewModel = createViewModel(id = 7L)
        advanceUntilIdle()

        viewModel.save("name", "body", ReasoningLevel.DEFAULT, LlmVendor.GROQ, "m")
        viewModel.save("name", "body", ReasoningLevel.DEFAULT, LlmVendor.GROQ, "m")
        advanceUntilIdle()

        coVerify(exactly = 1) { repository.save(any()) }
    }

    @Test
    fun `save persists vendor and model`() = runTest(testDispatcher) {
        templatesFlow.value = listOf(template)
        val viewModel = createViewModel(id = 7L)
        advanceUntilIdle()

        viewModel.saved.test {
            viewModel.save(
                "name",
                "body",
                ReasoningLevel.DEFAULT,
                LlmVendor.DEEPSEEK,
                "deepseek-reasoner",
            )
            awaitItem()
        }

        coVerify {
            repository.save(
                match { it.vendor == LlmVendor.DEEPSEEK && it.model == "deepseek-reasoner" },
            )
        }
    }

    @Test
    fun `save falls back to the vendor default model when model is blank`() =
        runTest(testDispatcher) {
            templatesFlow.value = listOf(template)
            val viewModel = createViewModel(id = 7L)
            advanceUntilIdle()

            viewModel.saved.test {
                viewModel.save("name", "body", ReasoningLevel.DEFAULT, LlmVendor.DEEPSEEK, "   ")
                awaitItem()
            }

            coVerify { repository.save(match { it.model == LlmVendor.DEEPSEEK.defaultModel }) }
        }

    @Test
    fun `modelForVendorChange replaces a blank model with the new vendor default`() {
        val viewModel = createViewModel(id = -1L)

        assertEquals(
            "deepseek-chat",
            viewModel.modelForVendorChange("", LlmVendor.DEEPSEEK),
        )
        assertEquals(
            "deepseek-chat",
            viewModel.modelForVendorChange("   ", LlmVendor.DEEPSEEK),
        )
    }

    @Test
    fun `modelForVendorChange swaps the other vendor default for the new vendor default`() {
        val viewModel = createViewModel(id = -1L)

        assertEquals(
            "deepseek-chat",
            viewModel.modelForVendorChange("openai/gpt-oss-120b", LlmVendor.DEEPSEEK),
        )
        assertEquals(
            "openai/gpt-oss-120b",
            viewModel.modelForVendorChange("deepseek-chat", LlmVendor.GROQ),
        )
    }

    @Test
    fun `modelForVendorChange keeps a user customized model`() {
        val viewModel = createViewModel(id = -1L)

        assertEquals(
            "deepseek-reasoner",
            viewModel.modelForVendorChange("deepseek-reasoner", LlmVendor.GROQ),
        )
        assertEquals(
            "llama-3.3-70b-versatile",
            viewModel.modelForVendorChange("llama-3.3-70b-versatile", LlmVendor.DEEPSEEK),
        )
    }

    @Test
    fun `modelForVendorChange keeps the model when it already is the new vendor default`() {
        val viewModel = createViewModel(id = -1L)

        assertEquals(
            "deepseek-chat",
            viewModel.modelForVendorChange("deepseek-chat", LlmVendor.DEEPSEEK),
        )
    }
}
