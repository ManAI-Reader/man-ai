package com.highliuk.manai.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.highliuk.manai.domain.chat.PromptContext
import com.highliuk.manai.domain.chat.PromptTemplateRenderer
import com.highliuk.manai.domain.model.ChatRole
import com.highliuk.manai.domain.model.PageRegion
import com.highliuk.manai.domain.model.PromptTemplate
import com.highliuk.manai.domain.repository.ChatRepository
import com.highliuk.manai.domain.repository.MangaRepository
import com.highliuk.manai.domain.repository.OcrCacheRepository
import com.highliuk.manai.domain.repository.PromptTemplateRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Launch-time inputs for a conversation that are resolved by the UI layer:
 * the user selection, the current translation and the localized fallback
 * strings used when a balloon merge tag has no content.
 */
data class ChatLaunchOptions(
    val selection: String? = null,
    val translation: String? = null,
    val noPageBalloonsFallback: String,
    val noPreviousBalloonsFallback: String,
)

/**
 * Starts a chat conversation from a manga page region: creates the
 * conversation (snapshotting the template's reasoning level), appends the
 * rendered prompt as the first user message and emits a navigation event with
 * the new conversation id.
 */
@HiltViewModel
class ChatLauncherViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    promptTemplateRepository: PromptTemplateRepository,
    private val mangaRepository: MangaRepository,
    private val ocrCacheRepository: OcrCacheRepository,
) : ViewModel() {

    val promptTemplates: StateFlow<List<PromptTemplate>> = promptTemplateRepository.observeTemplates()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    private val _navigateToChat = MutableSharedFlow<Long>()
    val navigateToChat: SharedFlow<Long> = _navigateToChat

    fun startConversation(
        template: PromptTemplate,
        region: PageRegion,
        mangaId: Long,
        options: ChatLaunchOptions,
    ) {
        val text = region.ocrText ?: return
        viewModelScope.launch {
            // Resolve the context (which may walk previous pages) before touching
            // the repository so cancellation cannot leave an empty conversation.
            val content = PromptTemplateRenderer.render(
                template.template,
                buildPromptContext(template.template, region, mangaId, options),
            )
            val conversationId = chatRepository.createConversation(
                title = buildTitle(text),
                mangaId = mangaId,
                pageIndex = region.pageIndex,
                regionIndex = region.regionIndex,
                reasoningLevel = template.reasoningLevel,
            )
            chatRepository.appendMessage(
                conversationId = conversationId,
                role = ChatRole.USER,
                content = content,
            )
            _navigateToChat.emit(conversationId)
        }
    }

    private suspend fun buildPromptContext(
        template: String,
        region: PageRegion,
        mangaId: Long,
        options: ChatLaunchOptions,
    ): PromptContext = PromptContext(
        text = region.ocrText.orEmpty(),
        selection = options.selection,
        translation = options.translation,
        title = fetchTitle(template, mangaId),
        sourceRegionIndex = region.regionIndex,
        pageRegions = fetchPageRegions(template, mangaId, region.pageIndex),
        previousPageRegions = fetchPreviousPageRegions(template, mangaId, region.pageIndex),
        noPageBalloonsFallback = options.noPageBalloonsFallback,
        noPreviousBalloonsFallback = options.noPreviousBalloonsFallback,
    )

    private suspend fun fetchTitle(template: String, mangaId: Long): String =
        if (PromptTemplateRenderer.usesTitle(template)) {
            mangaRepository.getMangaById(mangaId).first()?.title.orEmpty()
        } else {
            ""
        }

    private suspend fun fetchPageRegions(template: String, mangaId: Long, pageIndex: Int): List<PageRegion> =
        if (PromptTemplateRenderer.usesBalloons(template)) {
            ocrCacheRepository.getRegions(mangaId, pageIndex)
        } else {
            emptyList()
        }

    /**
     * Walks backwards from the page before [pageIndex] and returns the
     * regions of the nearest page that has at least one OCR'd balloon, or an
     * empty list when no previous page has any.
     */
    private suspend fun fetchPreviousPageRegions(
        template: String,
        mangaId: Long,
        pageIndex: Int,
    ): List<PageRegion> {
        if (!PromptTemplateRenderer.usesPreviousBalloons(template)) return emptyList()
        var found: List<PageRegion> = emptyList()
        var page = pageIndex - 1
        while (page >= 0 && found.isEmpty()) {
            found = ocrCacheRepository.getRegions(mangaId, page)
                .filter { !it.ocrText.isNullOrBlank() }
            page--
        }
        return found
    }

    internal fun buildTitle(text: String): String =
        text.lineSequence().first().take(TITLE_MAX).ifBlank { "…" }

    private companion object {
        const val TITLE_MAX = 40
        const val STOP_TIMEOUT_MS = 5000L
    }
}
