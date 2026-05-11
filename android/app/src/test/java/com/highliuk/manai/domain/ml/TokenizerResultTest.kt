package com.highliuk.manai.domain.ml

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TokenizerResultTest {

    @Test
    fun `tokenizerResult stores surface and reading`() {
        val result = TokenizerResult(surface = "食べる", reading = "タベル")
        assertEquals("食べる", result.surface)
        assertEquals("タベル", result.reading)
    }

    @Test
    fun `tokenizerResult with null reading`() {
        val result = TokenizerResult(surface = "Hello", reading = null)
        assertEquals("Hello", result.surface)
        assertNull(result.reading)
    }

    @Test
    fun `tokenizerResult data class equality`() {
        val a = TokenizerResult(surface = "食", reading = "ショク")
        val b = TokenizerResult(surface = "食", reading = "ショク")
        assertEquals(a, b)
    }
}
