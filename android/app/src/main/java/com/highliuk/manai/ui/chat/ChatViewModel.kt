package com.highliuk.manai.ui.chat

import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.highliuk.manai.R
import com.highliuk.manai.domain.furigana.FuriganaPipeline
import com.highliuk.manai.domain.furigana.FuriganaRunCache
import com.highliuk.manai.domain.llm.LlmFailure
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject

/**
 * Localized, user-facing chat error: a string resource plus the HTTP status
 * argument when the failure was a non-2xx response.
 */
data class ChatUiError(
    @param:StringRes val messageRes: Int,
    val httpStatus: Int? = null,
)

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

    private val _error = MutableStateFlow<ChatUiError?>(null)
    val error: StateFlow<ChatUiError?> = _error.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _truncated = MutableStateFlow(false)

    /** True when the last completed reply was cut short by the provider's token limit. */
    val truncated: StateFlow<Boolean> = _truncated.asStateFlow()

    private val _deleted = MutableSharedFlow<Unit>()

    /** Emits once the conversation has been deleted on user request. */
    val deleted: SharedFlow<Unit> = _deleted

    private var deletedExplicitly = false

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
     * Deletes the conversation on user request and signals the UI to leave.
     * Any in-flight generation is cancelled first so its final append cannot
     * land on cascade-deleted messages.
     */
    fun deleteConversation() {
        deletedExplicitly = true
        generationJob?.cancel()
        viewModelScope.launch {
            chatRepository.deleteConversation(conversationId)
            _deleted.emit(Unit)
        }
    }

    /**
     * Accepts at most one generation at a time: [_isGenerating] is set synchronously,
     * before any suspension, so rapid repeated calls cannot both pass the guard.
     */
    private var generationJob: Job? = null

    private fun startGeneration(block: suspend () -> Unit) {
        if (_isGenerating.value) return
        _isGenerating.value = true
        generationJob = viewModelScope.launch {
            try {
                block()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (expected: Exception) {
                _streamingText.value = null
                failWith(expected)
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
        _truncated.value = false
        generateChatReply(conversationId)
            .catch { throwable ->
                _streamingText.value = null
                failWith(throwable)
            }
            .collect { event ->
                when (event) {
                    is ChatGenerationEvent.Delta -> _streamingText.value = event.accumulatedText
                    is ChatGenerationEvent.Done -> {
                        _streamingText.value = null
                        _truncated.value = event.truncated
                    }
                    is ChatGenerationEvent.Error -> {
                        _streamingText.value = null
                        failWith(event.failure)
                    }
                }
            }
    }

    private fun failWith(throwable: Throwable) {
        logger.e(TAG, "Chat generation failed", throwable)
        _error.value = if (throwable is IOException) {
            ChatUiError(R.string.chat_error_network)
        } else {
            ChatUiError(R.string.chat_error_generic)
        }
    }

    private fun failWith(failure: LlmFailure) {
        logger.e(TAG, "LLM generation failed: $failure", null)
        _error.value = when (failure) {
            LlmFailure.Network -> ChatUiError(R.string.chat_error_network)
            is LlmFailure.Http -> ChatUiError(R.string.chat_error_http, failure.status)
            is LlmFailure.Generic -> ChatUiError(R.string.chat_error_generic)
        }
    }

    /**
     * Deletes the conversation when it never received an assistant reply, so
     * abandoned first generations don't leave husks in the conversation list.
     * Called from [onCleared]; also callable directly for testing.
     */
    internal suspend fun deleteIfOrphaned() {
        if (deletedExplicitly) return
        try {
            val messages = chatRepository.getMessages(conversationId)
            if (messages.none { it.role == ChatRole.ASSISTANT }) {
                chatRepository.deleteConversation(conversationId)
            }
        } catch (expected: Exception) {
            logger.e(TAG, "Orphan conversation cleanup failed", expected)
        }
    }

    override fun onCleared() {
        super.onCleared()
        // Blocking mirrors ReaderViewModel.onCleared: the scope is gone and
        // the delete must complete before the ViewModel is discarded.
        runBlocking {
            deleteIfOrphaned()
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5000L
        const val TAG = "ChatViewModel"
    }
}
