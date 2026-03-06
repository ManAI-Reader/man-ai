package com.highliuk.manai.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AppLanguageTest {
    @Test
    fun `SYSTEM has null tag`() {
        assertNull(AppLanguage.SYSTEM.tag)
    }

    @Test
    fun `ENGLISH has en tag`() {
        assertEquals("en", AppLanguage.ENGLISH.tag)
    }

    @Test
    fun `ITALIAN has it tag`() {
        assertEquals("it", AppLanguage.ITALIAN.tag)
    }

    @Test
    fun `JAPANESE has ja tag`() {
        assertEquals("ja", AppLanguage.JAPANESE.tag)
    }

    @Test
    fun `SPANISH has es tag`() {
        assertEquals("es", AppLanguage.SPANISH.tag)
    }

    @Test
    fun `PORTUGUESE_BR has pt-BR tag`() {
        assertEquals("pt-BR", AppLanguage.PORTUGUESE_BR.tag)
    }

    @Test
    fun `FRENCH has fr tag`() {
        assertEquals("fr", AppLanguage.FRENCH.tag)
    }

    @Test
    fun `CHINESE_SIMPLIFIED has zh-Hans tag`() {
        assertEquals("zh-Hans", AppLanguage.CHINESE_SIMPLIFIED.tag)
    }

    @Test
    fun `KOREAN has ko tag`() {
        assertEquals("ko", AppLanguage.KOREAN.tag)
    }

    @Test
    fun `GERMAN has de tag`() {
        assertEquals("de", AppLanguage.GERMAN.tag)
    }

    @Test
    fun `RUSSIAN has ru tag`() {
        assertEquals("ru", AppLanguage.RUSSIAN.tag)
    }

    @Test
    fun `INDONESIAN has id tag`() {
        assertEquals("id", AppLanguage.INDONESIAN.tag)
    }

    @Test
    fun `THAI has th tag`() {
        assertEquals("th", AppLanguage.THAI.tag)
    }

    @Test
    fun `POLISH has pl tag`() {
        assertEquals("pl", AppLanguage.POLISH.tag)
    }

    @Test
    fun `has exactly 14 entries`() {
        assertEquals(14, AppLanguage.entries.size)
    }
}
