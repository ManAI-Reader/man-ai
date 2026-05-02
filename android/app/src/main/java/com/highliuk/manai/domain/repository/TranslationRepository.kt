package com.highliuk.manai.domain.repository

import com.highliuk.manai.domain.model.TranslationResult

interface TranslationRepository {
    suspend fun translate(
        mangaId: Long,
        pageIndex: Int,
        regionIndex: Int,
        sourceText: String,
    ): TranslationResult

    suspend fun getCachedTranslation(
        mangaId: Long,
        pageIndex: Int,
        regionIndex: Int,
        sourceText: String,
    ): String?
}
