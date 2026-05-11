package com.highliuk.manai.domain.furigana

import org.junit.Assert.assertEquals
import org.junit.Test

class RendakuUtilsTest {

    @Test
    fun `rendaku of ka-row produces dakuten only`() {
        val result = RendakuUtils.rendaku("かわ")
        assertEquals(setOf("かわ", "がわ"), result)
    }

    @Test
    fun `rendaku of sa-row produces dakuten only`() {
        val result = RendakuUtils.rendaku("さけ")
        assertEquals(setOf("さけ", "ざけ"), result)
    }

    @Test
    fun `rendaku of ha-row produces both dakuten and handakuten`() {
        val result = RendakuUtils.rendaku("はな")
        assertEquals(setOf("はな", "ばな", "ぱな"), result)
    }

    @Test
    fun `rendaku with katakana`() {
        val result = RendakuUtils.rendaku("カワ")
        assertEquals(setOf("カワ", "ガワ"), result)
    }

    @Test
    fun `rendaku of katakana ha-row produces both variants`() {
        val result = RendakuUtils.rendaku("ハナ")
        assertEquals(setOf("ハナ", "バナ", "パナ"), result)
    }

    @Test
    fun `rendaku of non-voiceable char returns only original`() {
        val result = RendakuUtils.rendaku("あい")
        assertEquals(setOf("あい"), result)
    }

    @Test
    fun `rendaku of single char`() {
        val result = RendakuUtils.rendaku("か")
        assertEquals(setOf("か", "が"), result)
    }
}
