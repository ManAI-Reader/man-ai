package com.highliuk.manai.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.highliuk.manai.domain.model.Conversation
import com.highliuk.manai.domain.repository.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ConversationListViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
) : ViewModel() {

    val conversations: StateFlow<List<Conversation>> = chatRepository.observeConversations()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _pendingDelete = MutableStateFlow<Conversation?>(null)
    val pendingDelete: StateFlow<Conversation?> = _pendingDelete.asStateFlow()

    fun requestDelete(conversation: Conversation) {
        _pendingDelete.value = conversation
    }

    fun dismissDelete() {
        _pendingDelete.value = null
    }

    fun confirmDelete() {
        val target = _pendingDelete.value ?: return
        viewModelScope.launch {
            chatRepository.deleteConversation(target.id)
            _pendingDelete.value = null
        }
    }
}
