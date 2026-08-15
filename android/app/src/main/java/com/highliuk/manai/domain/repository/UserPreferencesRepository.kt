package com.highliuk.manai.domain.repository

import com.highliuk.manai.domain.model.AppLanguage
import com.highliuk.manai.domain.model.ReadingMode
import com.highliuk.manai.domain.model.TargetLanguage
import com.highliuk.manai.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow

interface UserPreferencesRepository {

    companion object {
        const val DEFAULT_LLM_BASE_URL = "https://api.groq.com/openai/v1"
        const val DEFAULT_LLM_MODEL = "llama-3.3-70b-versatile"
    }

    val gridColumns: Flow<Int>
    suspend fun setGridColumns(columns: Int)

    val gridColumnsLandscape: Flow<Int>
    suspend fun setGridColumnsLandscape(columns: Int)

    val readingMode: Flow<ReadingMode>
    suspend fun setReadingMode(mode: ReadingMode)

    val themeMode: Flow<ThemeMode>
    suspend fun setThemeMode(mode: ThemeMode)

    val appLanguage: Flow<AppLanguage>
    suspend fun setAppLanguage(language: AppLanguage)

    val ocrFontScale: Flow<Float>
    suspend fun setOcrFontScale(scale: Float)

    val tapToNavigate: Flow<Boolean>
    suspend fun setTapToNavigate(enabled: Boolean)

    val tapToNavigatePortrait: Flow<Boolean>
    suspend fun setTapToNavigatePortrait(enabled: Boolean)

    val tapToNavigateLandscape: Flow<Boolean>
    suspend fun setTapToNavigateLandscape(enabled: Boolean)

    val translationTargetLang: Flow<TargetLanguage>
    suspend fun setTranslationTargetLang(lang: TargetLanguage)

    val showFurigana: Flow<Boolean>
    suspend fun setShowFurigana(enabled: Boolean)

    val llmBaseUrl: Flow<String>
    suspend fun setLlmBaseUrl(url: String)

    val llmModel: Flow<String>
    suspend fun setLlmModel(model: String)

    val promptDefaultsSeeded: Flow<Boolean>
    suspend fun setPromptDefaultsSeeded()
}
