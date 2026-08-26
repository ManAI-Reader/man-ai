package com.highliuk.manai.ui.prompts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.highliuk.manai.domain.model.PromptTemplate
import com.highliuk.manai.domain.repository.PromptTemplateRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PromptListViewModel @Inject constructor(
    private val repository: PromptTemplateRepository,
) : ViewModel() {

    val templates: StateFlow<List<PromptTemplate>> = repository.observeTemplates()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _pendingDelete = MutableStateFlow<PromptTemplate?>(null)
    val pendingDelete: StateFlow<PromptTemplate?> = _pendingDelete.asStateFlow()

    fun requestDelete(template: PromptTemplate) {
        _pendingDelete.value = template
    }

    fun dismissDelete() {
        _pendingDelete.value = null
    }

    fun confirmDelete() {
        val target = _pendingDelete.value ?: return
        viewModelScope.launch {
            repository.delete(target.id)
            _pendingDelete.value = null
        }
    }
}
