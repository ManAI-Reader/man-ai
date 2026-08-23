package com.highliuk.manai.ui.chat

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.highliuk.manai.domain.furigana.FuriganaPipeline
import com.highliuk.manai.domain.furigana.FuriganaRunCache
import com.highliuk.manai.domain.logging.Logger
import com.highliuk.manai.domain.model.ChatMessage
import com.highliuk.manai.domain.model.ChatRole
import com.highliuk.manai.domain.model.Conversation
import com.highliuk.manai.domain.model.FuriganaToken
import com.highliuk.manai.domain.repository.ChatRepository
import com.highliuk.manai.domain.usecase.ChatGenerationEvent
import com.highliuk.manai.domain.usecase.GenerateChatReplyUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val chatRepository: ChatRepository,
    private val generateChatReply: GenerateChatReplyUseCase,
    private val furiganaPipeline: FuriganaPipeline,
    private val logger: Logger,
) : ViewModel() {

    private val conversationId: Long = savedStateHandle["conversationId"] ?: 0L

    val conversation: StateFlow<Conversation?> = chatRepository.observeConversation(conversationId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), null)

    val messages: StateFlow<List<ChatMessage>> = chatRepository.observeMessages(conversationId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    private val _streamingText = MutableStateFlow<String?>(null)
    val streamingText: StateFlow<String?> = _streamingText.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    init {
        startGeneration {
            generateIfLastMessageIsFromUser()
        }
    }

    fun sendMessage(text: String) {
        if (text.isBlank()) return
        startGeneration {
            chatRepository.appendMessage(conversationId, ChatRole.USER, text.trim())
            runGeneration()
        }
    }

    private val tokenizerReady: Deferred<Unit> = viewModelScope.async(
        context = Dispatchers.IO,
        start = CoroutineStart.LAZY,
    ) { furiganaPipeline.tokenizer.init() }

    private val kanjiReadingsReady: Deferred<Unit> = viewModelScope.async(
        context = Dispatchers.IO,
        start = CoroutineStart.LAZY,
    ) { furiganaPipeline.kanjiReadings.load() }

    private val furiganaRunCache = FuriganaRunCache { text ->
        withContext(Dispatchers.Default) { furiganaPipeline.parseFurigana(text) }
    }

    /**
     * Resolves furigana tokens for a closed Japanese run of streamed or
     * persisted assistant text. The tokenizer and readings are initialized
     * lazily on the first call; each distinct run is parsed exactly once.
     * Cancellation propagates so callers never mistake it for a result.
     */
    suspend fun resolveFurigana(text: String): List<FuriganaToken> = try {
        tokenizerReady.await()
        kanjiReadingsReady.await()
        furiganaRunCache.get(text)
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (expected: Exception) {
        logger.e("ChatViewModel", "Furigana parsing failed", expected)
        emptyList()
    }

    fun retry() {
        _error.value = null
        startGeneration {
            generateIfLastMessageIsFromUser()
        }
    }

    /**
     * Accepts at most one generation at a time: [_isGenerating] is set synchronously,
     * before any suspension, so rapid repeated calls cannot both pass the guard.
     */
    private fun startGeneration(block: suspend () -> Unit) {
        if (_isGenerating.value) return
        _isGenerating.value = true
        viewModelScope.launch {
            try {
                block()
            } catch (expected: Exception) {
                _streamingText.value = null
                _error.value = expected.message.orEmpty()
            } finally {
                _isGenerating.value = false
            }
        }
    }

    private suspend fun generateIfLastMessageIsFromUser() {
        if (chatRepository.getMessages(conversationId).lastOrNull()?.role == ChatRole.USER) {
            runGeneration()
        }
    }

    private suspend fun runGeneration() {
        _error.value = null
        generateChatReply(conversationId)
            .catch { throwable ->
                _streamingText.value = null
                _error.value = throwable.message.orEmpty()
            }
            .collect { event ->
                when (event) {
                    is ChatGenerationEvent.Delta -> _streamingText.value = event.accumulatedText
                    is ChatGenerationEvent.Done -> _streamingText.value = null
                    is ChatGenerationEvent.Error -> {
                        _streamingText.value = null
                        _error.value = event.message
                    }
                }
            }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5000L
    }
}
