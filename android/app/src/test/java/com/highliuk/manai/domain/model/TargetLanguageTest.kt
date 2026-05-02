package com.highliuk.manai.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class TargetLanguageTest {

    @Test
    fun `EN is the first entry`() {
        assertEquals("EN", TargetLanguage.EN.code)
    }

    @Test
    fun `all 10 languages are present`() {
        assertEquals(10, TargetLanguage.entries.size)
    }

    @Test
    fun `fromCode returns matching language`() {
        assertEquals(TargetLanguage.IT, TargetLanguage.fromCode("IT"))
    }

    @Test
    fun `fromCode returns EN for unknown code`() {
        assertEquals(TargetLanguage.EN, TargetLanguage.fromCode("XX"))
    }

    @Test
    fun `displayName uses native autoglossonyms`() {
        assertEquals("English", TargetLanguage.EN.displayName)
        assertEquals("Italiano", TargetLanguage.IT.displayName)
        assertEquals("Español", TargetLanguage.ES.displayName)
        assertEquals("Português (Brasil)", TargetLanguage.PT_BR.displayName)
        assertEquals("Français", TargetLanguage.FR.displayName)
        assertEquals("Deutsch", TargetLanguage.DE.displayName)
        assertEquals("中文", TargetLanguage.ZH.displayName)
        assertEquals("한국어", TargetLanguage.KO.displayName)
        assertEquals("Русский", TargetLanguage.RU.displayName)
        assertEquals("Polski", TargetLanguage.PL.displayName)
    }
}
