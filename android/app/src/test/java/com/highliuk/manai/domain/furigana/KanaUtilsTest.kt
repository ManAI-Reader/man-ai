package com.highliuk.manai.domain.furigana

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KanaUtilsTest {

    @Test
    fun `isKanji returns true for kanji`() {
        assertTrue(KanaUtils.isKanji('食'))
    }

    @Test
    fun `isKanji returns false for hiragana`() {
        assertFalse(KanaUtils.isKanji('あ'))
    }

    @Test
    fun `isKanji returns true for dounojiten`() {
        assertTrue(KanaUtils.isKanji('々'))
    }

    @Test
    fun `isKana returns true for hiragana`() {
        assertTrue(KanaUtils.isKana('あ'))
    }

    @Test
    fun `isKana returns true for katakana`() {
        assertTrue(KanaUtils.isKana('ア'))
    }

    @Test
    fun `isKana returns false for kanji`() {
        assertFalse(KanaUtils.isKana('食'))
    }

    @Test
    fun `toKatakana converts hiragana to katakana`() {
        assertEquals("アイウ", KanaUtils.toKatakana("あいう"))
    }

    @Test
    fun `toHiragana converts katakana to hiragana`() {
        assertEquals("あいう", KanaUtils.toHiragana("アイウ"))
    }

    @Test
    fun `isJapanese returns true for kanji`() {
        assertTrue(KanaUtils.isJapanese('食'))
    }

    @Test
    fun `isJapanese returns true for hiragana`() {
        assertTrue(KanaUtils.isJapanese('あ'))
    }

    @Test
    fun `isJapanese returns false for latin`() {
        assertFalse(KanaUtils.isJapanese('A'))
    }

    @Test
    fun `isKatakana returns true for katakana string`() {
        assertTrue(KanaUtils.isKatakana("カタカナ"))
    }

    @Test
    fun `isKatakana returns false for hiragana string`() {
        assertFalse(KanaUtils.isKatakana("あ"))
    }

    @Test
    fun `toKatakana leaves katakana unchanged`() {
        assertEquals("カタカナ", KanaUtils.toKatakana("カタカナ"))
    }

    @Test
    fun `toHiragana leaves hiragana unchanged`() {
        assertEquals("あいう", KanaUtils.toHiragana("あいう"))
    }
}
