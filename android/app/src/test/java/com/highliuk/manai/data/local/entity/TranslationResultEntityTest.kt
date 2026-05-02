package com.highliuk.manai.data.local.entity

import org.junit.Assert.assertEquals
import org.junit.Test

class TranslationResultEntityTest {

    @Test
    fun `entity stores all fields`() {
        val entity = TranslationResultEntity(
            mangaId = 1L,
            pageIndex = 2,
            regionIndex = 3,
            provider = "deepl",
            sourceText = "テスト",
            translatedText = "Test",
            targetLang = "EN",
            timestamp = 1000L,
        )

        assertEquals(1L, entity.mangaId)
        assertEquals(2, entity.pageIndex)
        assertEquals(3, entity.regionIndex)
        assertEquals("deepl", entity.provider)
        assertEquals("テスト", entity.sourceText)
        assertEquals("Test", entity.translatedText)
        assertEquals("EN", entity.targetLang)
        assertEquals(1000L, entity.timestamp)
    }

    @Test
    fun `entity equality works by value`() {
        val a = TranslationResultEntity(1L, 0, 0, "deepl", "テスト", "Test", "EN", 1000L)
        val b = TranslationResultEntity(1L, 0, 0, "deepl", "テスト", "Test", "EN", 1000L)

        assertEquals(a, b)
    }
}
