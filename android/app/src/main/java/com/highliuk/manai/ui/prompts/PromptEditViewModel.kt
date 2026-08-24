package com.highliuk.manai.ui.prompts

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.highliuk.manai.R
import com.highliuk.manai.domain.model.PromptTemplate
import com.highliuk.manai.domain.model.ReasoningLevel
import com.highliuk.manai.domain.repository.PromptTemplateRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Backs the full-screen prompt editor. The `id` navigation argument selects
 * the template to edit; [NEW_TEMPLATE_ID] (or a missing id) starts a new
 * template whose sort order follows the existing ones.
 */
@HiltViewModel
class PromptEditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: PromptTemplateRepository,
) : ViewModel() {

    private val templateId: Long = savedStateHandle["id"] ?: NEW_TEMPLATE_ID

    private val _template = MutableStateFlow<PromptTemplate?>(null)

    /** The template being edited, or null while it is still loading. */
    val template: StateFlow<PromptTemplate?> = _template.asStateFlow()

    private val _editError = MutableStateFlow<Int?>(null)

    /** String resource id of the current validation error, or null when valid. */
    val editError: StateFlow<Int?> = _editError.asStateFlow()

    private val _saved = MutableSharedFlow<Unit>()

    /** Emits once the template has been persisted so the UI can navigate back. */
    val saved: SharedFlow<Unit> = _saved

    private var saving = false

    init {
        viewModelScope.launch {
            val templates = repository.observeTemplates().first()
            _template.value = if (templateId == NEW_TEMPLATE_ID) {
                PromptTemplate(
                    id = 0L,
                    name = "",
                    template = "",
                    sortOrder = (templates.maxOfOrNull { it.sortOrder } ?: -1) + 1,
                )
            } else {
                templates.firstOrNull { it.id == templateId }
            }
        }
    }

    fun save(name: String, template: String, reasoningLevel: ReasoningLevel) {
        val trimmedName = name.trim()
        val trimmedTemplate = template.trim()
        if (trimmedName.isEmpty() || trimmedTemplate.isEmpty()) {
            _editError.value = R.string.prompt_name_required
            return
        }
        val current = _template.value
        if (current == null || saving) return
        saving = true
        _editError.value = null
        viewModelScope.launch {
            repository.save(
                current.copy(
                    name = trimmedName,
                    template = trimmedTemplate,
                    reasoningLevel = reasoningLevel,
                )
            )
            _saved.emit(Unit)
        }
    }

    companion object {
        /** Navigation argument value that means "create a new template". */
        const val NEW_TEMPLATE_ID = -1L
    }
}
