package com.highliuk.manai.ui.chat

import app.cash.turbine.test
import com.highliuk.manai.domain.model.ChatRole
import com.highliuk.manai.domain.model.Manga
import com.highliuk.manai.domain.model.PageRegion
import com.highliuk.manai.domain.model.PromptTemplate
import com.highliuk.manai.domain.model.ReasoningLevel
import com.highliuk.manai.domain.repository.ChatRepository
import com.highliuk.manai.domain.repository.MangaRepository
import com.highliuk.manai.domain.repository.OcrCacheRepository
import com.highliuk.manai.domain.repository.PromptTemplateRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
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
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ChatLauncherViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val chatRepository: ChatRepository = mockk(relaxed = true)
    private val promptTemplateRepository: PromptTemplateRepository = mockk {
        coEvery { observeTemplates() } returns flowOf(emptyList())
    }
    private val mangaRepository: MangaRepository = mockk {
        every { getMangaById(any()) } returns flowOf(null)
    }
    private val ocrCacheRepository: OcrCacheRepository = mockk {
        coEvery { getRegions(any(), any()) } returns emptyList()
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

    private val options = ChatLaunchOptions(
        selection = null,
        translation = null,
        noPageBalloonsFallback = NO_PAGE_BALLOONS,
        noPreviousBalloonsFallback = NO_PREVIOUS_BALLOONS,
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() = ChatLauncherViewModel(
        chatRepository = chatRepository,
        promptTemplateRepository = promptTemplateRepository,
        mangaRepository = mangaRepository,
        ocrCacheRepository = ocrCacheRepository,
    )

    private fun otherRegion(index: Int, text: String?, pageIndex: Int = 12) = region.copy(
        regionIndex = index,
        ocrText = text,
        pageIndex = pageIndex,
    )

    private fun appendedContent(): String {
        val contents = mutableListOf<String>()
        coVerify { chatRepository.appendMessage(any(), ChatRole.USER, capture(contents)) }
        return contents.last()
    }

    @Test
    fun startConversationCreatesConversationWithRegionReference() = runTest {
        coEvery { chatRepository.createConversation(any(), any(), any(), any(), any()) } returns 42L
        val viewModel = createViewModel()

        viewModel.startConversation(template, region, mangaId = 5L, options = options)
        advanceUntilIdle()

        coVerify {
            chatRepository.createConversation(
                title = "私は食べる",
                mangaId = 5L,
                pageIndex = 12,
                regionIndex = 3,
                reasoningLevel = ReasoningLevel.DEFAULT,
            )
        }
    }

    @Test
    fun startConversationCopiesTemplateReasoningLevelToConversation() = runTest {
        coEvery { chatRepository.createConversation(any(), any(), any(), any(), any()) } returns 42L
        val viewModel = createViewModel()

        viewModel.startConversation(
            template = template.copy(reasoningLevel = ReasoningLevel.LOW),
            region = region,
            mangaId = 5L,
            options = options,
        )
        advanceUntilIdle()

        coVerify {
            chatRepository.createConversation(any(), any(), any(), any(), ReasoningLevel.LOW)
        }
    }

    @Test
    fun startConversationAppendsRenderedTemplateAsUserMessage() = runTest {
        coEvery { chatRepository.createConversation(any(), any(), any(), any(), any()) } returns 42L
        val viewModel = createViewModel()

        viewModel.startConversation(
            template = template,
            region = region,
            mangaId = 5L,
            options = options.copy(selection = "食べる", translation = "I eat"),
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
    fun startConversationInjectsMangaTitle() = runTest {
        coEvery { chatRepository.createConversation(any(), any(), any(), any(), any()) } returns 42L
        every { mangaRepository.getMangaById(5L) } returns flowOf(
            Manga(id = 5L, uri = "content://m", title = "よつばと!", pageCount = 100)
        )
        val viewModel = createViewModel()

        viewModel.startConversation(
            template = template.copy(template = "From {title}: {text}"),
            region = region,
            mangaId = 5L,
            options = options,
        )
        advanceUntilIdle()

        assertEquals("From よつばと!: 私は食べる", appendedContent())
    }

    @Test
    fun startConversationRendersOtherBalloonsOfSamePage() = runTest {
        coEvery { chatRepository.createConversation(any(), any(), any(), any(), any()) } returns 42L
        coEvery { ocrCacheRepository.getRegions(5L, 12) } returns listOf(
            otherRegion(0, "先の吹き出し"),
            otherRegion(3, "私は食べる"),
            otherRegion(5, "次の吹き出し"),
        )
        val viewModel = createViewModel()

        viewModel.startConversation(
            template = template.copy(template = "Page:\n{balloons}"),
            region = region,
            mangaId = 5L,
            options = options,
        )
        advanceUntilIdle()

        assertEquals("Page:\n- 先の吹き出し\n- 次の吹き出し", appendedContent())
    }

    @Test
    fun startConversationUsesNearestPreviousPageWithBalloons() = runTest {
        coEvery { chatRepository.createConversation(any(), any(), any(), any(), any()) } returns 42L
        coEvery { ocrCacheRepository.getRegions(5L, 11) } returns listOf(otherRegion(0, null, 11))
        coEvery { ocrCacheRepository.getRegions(5L, 10) } returns listOf(
            otherRegion(1, "前のページ", 10),
            otherRegion(0, "最初", 10),
        )
        val viewModel = createViewModel()

        viewModel.startConversation(
            template = template.copy(template = "{prev_balloons}"),
            region = region,
            mangaId = 5L,
            options = options,
        )
        advanceUntilIdle()

        assertEquals("- 最初\n- 前のページ", appendedContent())
        coVerify(exactly = 0) { ocrCacheRepository.getRegions(5L, 9) }
    }

    @Test
    fun startConversationFallsBackWhenNoPreviousPageHasBalloons() = runTest {
        coEvery { chatRepository.createConversation(any(), any(), any(), any(), any()) } returns 42L
        val viewModel = createViewModel()

        viewModel.startConversation(
            template = template.copy(template = "{prev_balloons}"),
            region = region.copy(pageIndex = 2),
            mangaId = 5L,
            options = options,
        )
        advanceUntilIdle()

        assertEquals(NO_PREVIOUS_BALLOONS, appendedContent())
        coVerify { ocrCacheRepository.getRegions(5L, 1) }
        coVerify { ocrCacheRepository.getRegions(5L, 0) }
    }

    @Test
    fun startConversationBuildsContextBeforeCreatingConversation() = runTest {
        coEvery { chatRepository.createConversation(any(), any(), any(), any(), any()) } returns 42L
        coEvery { ocrCacheRepository.getRegions(5L, 11) } returns listOf(
            otherRegion(0, "前のページ", 11),
        )
        val viewModel = createViewModel()

        viewModel.startConversation(
            template = template.copy(template = "{prev_balloons}"),
            region = region,
            mangaId = 5L,
            options = options,
        )
        advanceUntilIdle()

        coVerifyOrder {
            ocrCacheRepository.getRegions(5L, 11)
            chatRepository.createConversation(any(), any(), any(), any(), any())
            chatRepository.appendMessage(any(), any(), any())
        }
    }

    @Test
    fun startConversationSkipsContextFetchesWhenTagsAbsent() = runTest {
        coEvery { chatRepository.createConversation(any(), any(), any(), any(), any()) } returns 42L
        val viewModel = createViewModel()

        viewModel.startConversation(template, region, mangaId = 5L, options = options)
        advanceUntilIdle()

        coVerify(exactly = 0) { mangaRepository.getMangaById(any()) }
        coVerify(exactly = 0) { ocrCacheRepository.getRegions(any(), any()) }
    }

    @Test
    fun startConversationIsNoOpWhenOcrTextIsNull() = runTest {
        val viewModel = createViewModel()

        viewModel.startConversation(
            template = template,
            region = region.copy(ocrText = null),
            mangaId = 5L,
            options = options,
        )
        advanceUntilIdle()

        coVerify(exactly = 0) {
            chatRepository.createConversation(any(), any(), any(), any(), any())
        }
        coVerify(exactly = 0) { chatRepository.appendMessage(any(), any(), any()) }
    }

    @Test
    fun navigateToChatEmitsNewConversationId() = runTest {
        coEvery { chatRepository.createConversation(any(), any(), any(), any(), any()) } returns 99L
        val viewModel = createViewModel()

        viewModel.navigateToChat.test {
            viewModel.startConversation(template, region, mangaId = 5L, options = options)
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

    private companion object {
        const val NO_PAGE_BALLOONS = "No other balloons on this page"
        const val NO_PREVIOUS_BALLOONS = "No balloons on the previous pages"
    }
}
