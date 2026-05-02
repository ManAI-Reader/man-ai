package com.highliuk.manai.domain.translation

import com.highliuk.manai.domain.model.TranslationResult

interface TranslationProvider {
    val id: String
    suspend fun translate(text: String, targetLang: String): TranslationResult
}
