package com.highliuk.manai.ui.prompts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.highliuk.manai.R
import com.highliuk.manai.domain.model.PromptTemplate
import com.highliuk.manai.domain.model.ReasoningLevel
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

    private val _editing = MutableStateFlow<PromptTemplate?>(null)
    val editing: StateFlow<PromptTemplate?> = _editing.asStateFlow()

    private val _editError = MutableStateFlow<Int?>(null)

    /** String resource id of the current edit validation error, or null when valid. */
    val editError: StateFlow<Int?> = _editError.asStateFlow()

    fun requestNew() {
        val nextSortOrder = (templates.value.maxOfOrNull { it.sortOrder } ?: -1) + 1
        _editing.value = PromptTemplate(id = 0L, name = "", template = "", sortOrder = nextSortOrder)
        _editError.value = null
    }

    fun requestEdit(template: PromptTemplate) {
        _editing.value = template
        _editError.value = null
    }

    fun dismissEdit() {
        _editing.value = null
        _editError.value = null
    }

    fun saveTemplate(name: String, template: String, reasoningLevel: ReasoningLevel) {
        val trimmedName = name.trim()
        val trimmedTemplate = template.trim()
        if (trimmedName.isEmpty() || trimmedTemplate.isEmpty()) {
            _editError.value = R.string.prompt_name_required
            return
        }
        val current = _editing.value ?: return
        _editing.value = null
        _editError.value = null
        viewModelScope.launch {
            repository.save(
                current.copy(
                    name = trimmedName,
                    template = trimmedTemplate,
                    reasoningLevel = reasoningLevel,
                )
            )
        }
    }

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
