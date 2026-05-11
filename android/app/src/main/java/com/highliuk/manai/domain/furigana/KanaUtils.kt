package com.highliuk.manai.domain.furigana

object KanaUtils {

    fun isKanji(char: Char): Boolean =
        char in '一'..'鿿' || char == '々'

    fun isKana(char: Char): Boolean =
        isHiragana(char) || isKatakana(char)

    fun isJapanese(char: Char): Boolean =
        isKanji(char) || isKana(char)

    fun isKatakana(text: String): Boolean =
        text.isNotEmpty() && text.all { isKatakana(it) }

    fun toKatakana(text: String): String =
        text.map { if (isHiragana(it)) it + 0x60 else it }.joinToString("")

    fun toHiragana(text: String): String =
        text.map { if (isKatakana(it)) it - 0x60 else it }.joinToString("")

    private fun isHiragana(char: Char): Boolean =
        char in '぀'..'ゟ'

    private fun isKatakana(char: Char): Boolean =
        char in '゠'..'ヿ'
}
