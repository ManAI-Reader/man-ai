package com.highliuk.manai.ui.chat

import app.cash.turbine.test
import com.highliuk.manai.domain.model.ChatRole
import com.highliuk.manai.domain.model.PageRegion
import com.highliuk.manai.domain.model.PromptTemplate
import com.highliuk.manai.domain.repository.ChatRepository
import com.highliuk.manai.domain.repository.PromptTemplateRepository
import io.mockk.coEvery
import io.mockk.coVerify
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
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ChatLauncherViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val chatRepository: ChatRepository = mockk(relaxed = true)
    private val promptTemplateRepository: PromptTemplateRepository = mockk {
        coEvery { observeTemplates() } returns flowOf(emptyList())
    }

    private val template = PromptTemplate(
        id = 7L,
        name = "Explain",
        template = "Explain {selection} in {text}. Translation: {translation}",
    )

    private val region = PageRegion(
        regionIndex = 3,
        normX1 = 0f,
        normY1 = 0f,
        normX2 = 1f,
        normY2 = 1f,
        confidence = 0.9f,
        ocrText = "私は食べる",
        pageIndex = 12,
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() = ChatLauncherViewModel(chatRepository, promptTemplateRepository)

    @Test
    fun startConversationCreatesConversationWithRegionReference() = runTest {
        coEvery { chatRepository.createConversation(any(), any(), any(), any()) } returns 42L
        val viewModel = createViewModel()

        viewModel.startConversation(template, region, mangaId = 5L, selection = null, translation = null)
        advanceUntilIdle()

        coVerify {
            chatRepository.createConversation(
                title = "私は食べる",
                mangaId = 5L,
                pageIndex = 12,
                regionIndex = 3,
            )
        }
    }

    @Test
    fun startConversationAppendsRenderedTemplateAsUserMessage() = runTest {
        coEvery { chatRepository.createConversation(any(), any(), any(), any()) } returns 42L
        val viewModel = createViewModel()

        viewModel.startConversation(
            template = template,
            region = region,
            mangaId = 5L,
            selection = "食べる",
            translation = "I eat",
        )
        advanceUntilIdle()

        coVerify {
            chatRepository.appendMessage(
                conversationId = 42L,
                role = ChatRole.USER,
                content = "Explain 食べる in 私は食べる. Translation: I eat",
            )
        }
    }

    @Test
    fun startConversationIsNoOpWhenOcrTextIsNull() = runTest {
        val viewModel = createViewModel()

        viewModel.startConversation(
            template = template,
            region = region.copy(ocrText = null),
            mangaId = 5L,
            selection = null,
            translation = null,
        )
        advanceUntilIdle()

        coVerify(exactly = 0) { chatRepository.createConversation(any(), any(), any(), any()) }
        coVerify(exactly = 0) { chatRepository.appendMessage(any(), any(), any()) }
    }

    @Test
    fun navigateToChatEmitsNewConversationId() = runTest {
        coEvery { chatRepository.createConversation(any(), any(), any(), any()) } returns 99L
        val viewModel = createViewModel()

        viewModel.navigateToChat.test {
            viewModel.startConversation(template, region, mangaId = 5L, selection = null, translation = null)
            advanceUntilIdle()
            assertEquals(99L, awaitItem())
        }
    }

    @Test
    fun buildTitleUsesFirstLineTruncatedTo40Chars() {
        val viewModel = createViewModel()

        val longFirstLine = "あ".repeat(50) + "\nsecond line"

        assertEquals("あ".repeat(40), viewModel.buildTitle(longFirstLine))
        assertEquals("短い", viewModel.buildTitle("短い\n二行目"))
    }

    @Test
    fun buildTitleFallsBackToEllipsisWhenFirstLineBlank() {
        val viewModel = createViewModel()

        assertEquals("…", viewModel.buildTitle("\nbody"))
    }
}
