package com.highliuk.manai.data.translation

import com.highliuk.manai.data.local.dao.TranslationResultDao
import com.highliuk.manai.data.local.entity.TranslationResultEntity
import com.highliuk.manai.domain.model.TargetLanguage
import com.highliuk.manai.domain.model.TranslationResult
import com.highliuk.manai.domain.translation.TranslationProvider
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TranslationRepositoryImplTest {

    private val dao = mockk<TranslationResultDao>(relaxed = true)
    private val provider = mockk<TranslationProvider>()
    private val targetLangFlow = MutableStateFlow(TargetLanguage.EN)

    private fun createRepository(): TranslationRepositoryImpl {
        every { provider.id } returns "deepl"
        return TranslationRepositoryImpl(dao, provider, targetLangFlow)
    }

    @Test
    fun `translate returns cached result when cache hit and text and lang match`() = runTest {
        val cached = TranslationResultEntity(
            mangaId = 1L, pageIndex = 0, regionIndex = 0, provider = "deepl",
            sourceText = "テスト", translatedText = "Test", targetLang = "EN",
            timestamp = 1000L,
        )
        coEvery { dao.get(1L, 0, 0, "deepl") } returns cached

        val repo = createRepository()
        val result = repo.translate(1L, 0, 0, "テスト")

        assertEquals(TranslationResult.Success("Test"), result)
        coVerify(exactly = 0) { provider.translate(any(), any()) }
    }

    @Test
    fun `translate calls provider on cache miss`() = runTest {
        coEvery { dao.get(1L, 0, 0, "deepl") } returns null
        coEvery { provider.translate("テスト", "EN") } returns TranslationResult.Success("Test")

        val repo = createRepository()
        val result = repo.translate(1L, 0, 0, "テスト")

        assertEquals(TranslationResult.Success("Test"), result)
        coVerify { provider.translate("テスト", "EN") }
    }

    @Test
    fun `translate caches success result`() = runTest {
        coEvery { dao.get(1L, 0, 0, "deepl") } returns null
        coEvery { provider.translate("テスト", "EN") } returns TranslationResult.Success("Test")

        val repo = createRepository()
        repo.translate(1L, 0, 0, "テスト")

        coVerify { dao.upsert(match { it.translatedText == "Test" && it.sourceText == "テスト" }) }
    }

    @Test
    fun `translate does not cache error result`() = runTest {
        coEvery { dao.get(1L, 0, 0, "deepl") } returns null
        coEvery { provider.translate("テスト", "EN") } returns TranslationResult.Error("fail")

        val repo = createRepository()
        repo.translate(1L, 0, 0, "テスト")

        coVerify(exactly = 0) { dao.upsert(any()) }
    }

    @Test
    fun `translate re-fetches when source text changed`() = runTest {
        val stale = TranslationResultEntity(
            mangaId = 1L, pageIndex = 0, regionIndex = 0, provider = "deepl",
            sourceText = "古いテキスト", translatedText = "Old", targetLang = "EN",
            timestamp = 1000L,
        )
        coEvery { dao.get(1L, 0, 0, "deepl") } returns stale
        coEvery { provider.translate("新しいテキスト", "EN") } returns TranslationResult.Success("New")

        val repo = createRepository()
        val result = repo.translate(1L, 0, 0, "新しいテキスト")

        assertEquals(TranslationResult.Success("New"), result)
    }

    @Test
    fun `translate re-fetches when target lang changed`() = runTest {
        val stale = TranslationResultEntity(
            mangaId = 1L, pageIndex = 0, regionIndex = 0, provider = "deepl",
            sourceText = "テスト", translatedText = "Test", targetLang = "EN",
            timestamp = 1000L,
        )
        coEvery { dao.get(1L, 0, 0, "deepl") } returns stale
        coEvery { provider.translate("テスト", "IT") } returns TranslationResult.Success("Prova")
        targetLangFlow.value = TargetLanguage.IT

        val repo = createRepository()
        val result = repo.translate(1L, 0, 0, "テスト")

        assertEquals(TranslationResult.Success("Prova"), result)
    }

    @Test
    fun `getCachedTranslation returns text when cache hit`() = runTest {
        val cached = TranslationResultEntity(
            mangaId = 1L, pageIndex = 0, regionIndex = 0, provider = "deepl",
            sourceText = "テスト", translatedText = "Test", targetLang = "EN",
            timestamp = 1000L,
        )
        coEvery { dao.get(1L, 0, 0, "deepl") } returns cached

        val repo = createRepository()
        val result = repo.getCachedTranslation(1L, 0, 0, "テスト")

        assertEquals("Test", result)
    }

    @Test
    fun `getCachedTranslation returns null on cache miss`() = runTest {
        coEvery { dao.get(1L, 0, 0, "deepl") } returns null

        val repo = createRepository()
        val result = repo.getCachedTranslation(1L, 0, 0, "テスト")

        assertNull(result)
    }
}
