package com.highliuk.manai.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TranslationResultTest {

    @Test
    fun `Success holds translated text`() {
        val result = TranslationResult.Success("Hello")

        assertTrue(result is TranslationResult.Success)
        assertEquals("Hello", result.text)
    }

    @Test
    fun `Error holds error message`() {
        val result = TranslationResult.Error("API key missing")

        assertTrue(result is TranslationResult.Error)
        assertEquals("API key missing", result.message)
    }

    @Test
    fun `Success and Error are distinct subtypes`() {
        val success: TranslationResult = TranslationResult.Success("text")
        val error: TranslationResult = TranslationResult.Error("err")

        assertTrue(success is TranslationResult.Success)
        assertTrue(error is TranslationResult.Error)
    }
}
