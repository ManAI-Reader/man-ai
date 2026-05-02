package com.highliuk.manai.data.translation

import com.highliuk.manai.data.local.dao.TranslationResultDao
import com.highliuk.manai.data.local.entity.TranslationResultEntity
import com.highliuk.manai.domain.model.TargetLanguage
import com.highliuk.manai.domain.model.TranslationResult
import com.highliuk.manai.domain.repository.TranslationRepository
import com.highliuk.manai.domain.translation.TranslationProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class TranslationRepositoryImpl @Inject constructor(
    private val dao: TranslationResultDao,
    private val provider: TranslationProvider,
    private val targetLangFlow: Flow<TargetLanguage>,
) : TranslationRepository {

    override suspend fun translate(
        mangaId: Long,
        pageIndex: Int,
        regionIndex: Int,
        sourceText: String,
    ): TranslationResult {
        val targetLang = targetLangFlow.first()
        val cached = dao.get(mangaId, pageIndex, regionIndex, provider.id)
        if (cached != null &&
            cached.sourceText == sourceText &&
            cached.targetLang == targetLang.code
        ) {
            return TranslationResult.Success(cached.translatedText)
        }

        val result = provider.translate(sourceText, targetLang.code)
        if (result is TranslationResult.Success) {
            dao.upsert(
                TranslationResultEntity(
                    mangaId = mangaId,
                    pageIndex = pageIndex,
                    regionIndex = regionIndex,
                    provider = provider.id,
                    sourceText = sourceText,
                    translatedText = result.text,
                    targetLang = targetLang.code,
                    timestamp = System.currentTimeMillis(),
                )
            )
        }
        return result
    }

    override suspend fun getCachedTranslation(
        mangaId: Long,
        pageIndex: Int,
        regionIndex: Int,
        sourceText: String,
    ): String? {
        val targetLang = targetLangFlow.first()
        val cached = dao.get(mangaId, pageIndex, regionIndex, provider.id)
        return if (cached != null &&
            cached.sourceText == sourceText &&
            cached.targetLang == targetLang.code
        ) {
            cached.translatedText
        } else {
            null
        }
    }
}
