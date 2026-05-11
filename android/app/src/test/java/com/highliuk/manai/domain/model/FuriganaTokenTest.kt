package com.highliuk.manai.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class FuriganaTokenTest {

    @Test
    fun `token stores surface reading and parts`() {
        val parts = listOf(
            FuriganaPart.kanji("食", "た"),
            FuriganaPart.kana("べ"),
            FuriganaPart.kana("る")
        )
        val token = FuriganaToken(
            surface = "食べる",
            reading = "タベル",
            parts = parts
        )

        assertEquals("食べる", token.surface)
        assertEquals("タベル", token.reading)
        assertEquals(3, token.parts.size)
    }

    @Test
    fun `token with null reading`() {
        val parts = listOf(FuriganaPart.kana("あ"))
        val token = FuriganaToken(
            surface = "あ",
            reading = null,
            parts = parts
        )

        assertEquals("あ", token.surface)
        assertEquals(null, token.reading)
    }

    @Test
    fun `token parts are accessible in order`() {
        val parts = listOf(
            FuriganaPart.kanji("食", "しょく"),
            FuriganaPart.kanji("堂", "どう")
        )
        val token = FuriganaToken(
            surface = "食堂",
            reading = "ショクドウ",
            parts = parts
        )

        assertEquals("食", token.parts[0].surface)
        assertEquals("しょく", token.parts[0].reading)
        assertEquals("堂", token.parts[1].surface)
        assertEquals("どう", token.parts[1].reading)
    }
}
