package com.highliuk.manai.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.highliuk.manai.domain.chat.PromptTemplateRenderer
import com.highliuk.manai.domain.model.ChatRole
import com.highliuk.manai.domain.model.PageRegion
import com.highliuk.manai.domain.model.PromptTemplate
import com.highliuk.manai.domain.repository.ChatRepository
import com.highliuk.manai.domain.repository.PromptTemplateRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Starts a chat conversation from a manga page region: creates the
 * conversation, appends the rendered prompt as the first user message and
 * emits a navigation event with the new conversation id.
 */
@HiltViewModel
class ChatLauncherViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    promptTemplateRepository: PromptTemplateRepository,
) : ViewModel() {

    val promptTemplates: StateFlow<List<PromptTemplate>> = promptTemplateRepository.observeTemplates()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    private val _navigateToChat = MutableSharedFlow<Long>()
    val navigateToChat: SharedFlow<Long> = _navigateToChat

    fun startConversation(
        template: PromptTemplate,
        region: PageRegion,
        mangaId: Long,
        selection: String?,
        translation: String?,
    ) {
        val text = region.ocrText ?: return
        viewModelScope.launch {
            val conversationId = chatRepository.createConversation(
                title = buildTitle(text),
                mangaId = mangaId,
                pageIndex = region.pageIndex,
                regionIndex = region.regionIndex,
            )
            chatRepository.appendMessage(
                conversationId = conversationId,
                role = ChatRole.USER,
                content = PromptTemplateRenderer.render(template.template, text, selection, translation),
            )
            _navigateToChat.emit(conversationId)
        }
    }

    internal fun buildTitle(text: String): String =
        text.lineSequence().first().take(TITLE_MAX).ifBlank { "…" }

    private companion object {
        const val TITLE_MAX = 40
        const val STOP_TIMEOUT_MS = 5000L
    }
}
