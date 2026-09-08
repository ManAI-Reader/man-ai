package com.highliuk.manai.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.highliuk.manai.domain.model.Conversation
import com.highliuk.manai.domain.repository.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ConversationListViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
) : ViewModel() {

    private val _isSearchActive = MutableStateFlow(false)
    val isSearchActive: StateFlow<Boolean> = _isSearchActive.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val conversations: StateFlow<List<Conversation>> =
        combine(_isSearchActive, _searchQuery) { active, query ->
            if (active) query else ""
        }
            .distinctUntilChanged()
            .flatMapLatest { effectiveQuery ->
                if (effectiveQuery.isBlank()) {
                    chatRepository.observeConversations()
                } else {
                    chatRepository.searchConversations(effectiveQuery)
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _pendingDelete = MutableStateFlow<Conversation?>(null)
    val pendingDelete: StateFlow<Conversation?> = _pendingDelete.asStateFlow()

    fun openSearch() {
        _isSearchActive.value = true
    }

    fun closeSearch() {
        _isSearchActive.value = false
        _searchQuery.value = ""
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

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
