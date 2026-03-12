package com.highliuk.manai.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.highliuk.manai.domain.model.AppLanguage
import com.highliuk.manai.domain.model.ReadingMode
import com.highliuk.manai.domain.model.ThemeMode
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

    val gridColumnsLandscape: StateFlow<Int> = userPreferencesRepository.gridColumnsLandscape
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 5)

    fun setGridColumnsLandscape(columns: Int) {
        viewModelScope.launch {
            userPreferencesRepository.setGridColumnsLandscape(columns)
        }
    }

    val readingMode: StateFlow<ReadingMode> = userPreferencesRepository.readingMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ReadingMode.LTR)

    fun setReadingMode(mode: ReadingMode) {
        viewModelScope.launch {
            userPreferencesRepository.setReadingMode(mode)
        }
    }

    val themeMode: StateFlow<ThemeMode> = userPreferencesRepository.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ThemeMode.SYSTEM)

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            userPreferencesRepository.setThemeMode(mode)
        }
    }

    val appLanguage: StateFlow<AppLanguage> = userPreferencesRepository.appLanguage
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppLanguage.SYSTEM)

    fun setAppLanguage(language: AppLanguage) {
        viewModelScope.launch {
            userPreferencesRepository.setAppLanguage(language)
        }
    }

    val ocrFontScale: StateFlow<Float> = userPreferencesRepository.ocrFontScale
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1.5f)

    fun setOcrFontScale(scale: Float) {
        viewModelScope.launch {
            userPreferencesRepository.setOcrFontScale(scale)
        }
    }

    val tapToNavigate: StateFlow<Boolean> = userPreferencesRepository.tapToNavigate
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun setTapToNavigate(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setTapToNavigate(enabled)
        }
    }
}
