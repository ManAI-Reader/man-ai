package com.highliuk.manai.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.highliuk.manai.data.llm.LlmCredentialsManager
import com.highliuk.manai.data.translation.DeepLCredentialsManager
import com.highliuk.manai.domain.model.AppLanguage
import com.highliuk.manai.domain.model.LlmVendor
import com.highliuk.manai.domain.model.ReadingMode
import com.highliuk.manai.domain.model.TargetLanguage
import com.highliuk.manai.domain.model.ThemeMode
import com.highliuk.manai.domain.repository.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val credentialsManager: DeepLCredentialsManager,
    private val llmCredentialsManager: LlmCredentialsManager,
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

    val tapToNavigatePortrait: StateFlow<Boolean> = userPreferencesRepository.tapToNavigatePortrait
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun setTapToNavigatePortrait(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setTapToNavigatePortrait(enabled)
        }
    }

    val tapToNavigateLandscape: StateFlow<Boolean> = userPreferencesRepository.tapToNavigateLandscape
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    fun setTapToNavigateLandscape(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setTapToNavigateLandscape(enabled)
        }
    }

    val translationTargetLang: StateFlow<TargetLanguage> =
        userPreferencesRepository.translationTargetLang
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TargetLanguage.EN)

    fun setTranslationTargetLang(lang: TargetLanguage) {
        viewModelScope.launch {
            userPreferencesRepository.setTranslationTargetLang(lang)
        }
    }

    val showFurigana: StateFlow<Boolean> = userPreferencesRepository.showFurigana
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun setShowFurigana(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setShowFurigana(enabled)
        }
    }

    private val _deeplApiKey = MutableStateFlow(credentialsManager.getApiKey().orEmpty())
    val deeplApiKey: StateFlow<String> = _deeplApiKey

    fun setDeeplApiKey(key: String) {
        if (key.isBlank()) {
            credentialsManager.clearApiKey()
            _deeplApiKey.value = ""
        } else {
            credentialsManager.saveApiKey(key)
            _deeplApiKey.value = key
        }
    }

    private val _groqApiKey =
        MutableStateFlow(llmCredentialsManager.getApiKey(LlmVendor.GROQ).orEmpty())
    val groqApiKey: StateFlow<String> = _groqApiKey.asStateFlow()

    fun setGroqApiKey(key: String) {
        setVendorApiKey(LlmVendor.GROQ, key, _groqApiKey)
    }

    private val _deepseekApiKey =
        MutableStateFlow(llmCredentialsManager.getApiKey(LlmVendor.DEEPSEEK).orEmpty())
    val deepseekApiKey: StateFlow<String> = _deepseekApiKey.asStateFlow()

    fun setDeepseekApiKey(key: String) {
        setVendorApiKey(LlmVendor.DEEPSEEK, key, _deepseekApiKey)
    }

    private fun setVendorApiKey(vendor: LlmVendor, key: String, state: MutableStateFlow<String>) {
        if (key.isBlank()) {
            llmCredentialsManager.clearApiKey(vendor)
            state.value = ""
        } else {
            llmCredentialsManager.saveApiKey(vendor, key)
            state.value = key
        }
    }
}
