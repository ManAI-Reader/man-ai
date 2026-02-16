package com.highliuk.manai.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.highliuk.manai.domain.model.ReadingMode
import com.highliuk.manai.domain.repository.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    val gridColumns: StateFlow<Int> = userPreferencesRepository.gridColumns
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 2)

    fun setGridColumns(columns: Int) {
        viewModelScope.launch {
            userPreferencesRepository.setGridColumns(columns)
        }
    }

    val readingMode: StateFlow<ReadingMode> = userPreferencesRepository.readingMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ReadingMode.LTR)

    fun setReadingMode(mode: ReadingMode) {
        viewModelScope.launch {
            userPreferencesRepository.setReadingMode(mode)
        }
    }
}
