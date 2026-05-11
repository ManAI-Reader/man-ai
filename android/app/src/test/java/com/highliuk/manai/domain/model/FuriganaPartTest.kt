package com.highliuk.manai.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FuriganaPartTest {

    @Test
    fun `kanji with different reading stores reading`() {
        val part = FuriganaPart.kanji("食", "た")
        assertEquals("食", part.surface)
        assertEquals("た", part.reading)
    }

    @Test
    fun `kanji with same surface and reading nullifies reading`() {
        val part = FuriganaPart.kanji("た", "た")
        assertEquals("た", part.surface)
        assertNull(part.reading)
    }

    @Test
    fun `kana has null reading`() {
        val part = FuriganaPart.kana("あ")
        assertEquals("あ", part.surface)
        assertNull(part.reading)
    }

    @Test
    fun `kanji with multi-char reading`() {
        val part = FuriganaPart.kanji("食", "しょく")
        assertEquals("食", part.surface)
        assertEquals("しょく", part.reading)
    }

    @Test
    fun `kana with katakana surface`() {
        val part = FuriganaPart.kana("ア")
        assertEquals("ア", part.surface)
        assertNull(part.reading)
    }
}
