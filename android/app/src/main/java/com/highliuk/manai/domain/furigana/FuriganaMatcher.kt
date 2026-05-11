package com.highliuk.manai.domain.furigana

import com.highliuk.manai.domain.model.FuriganaPart

class FuriganaMatcher(private val kanjiSplitter: KanjiSplitter) {

    fun match(surface: String, reading: String): List<FuriganaPart>? = when {
        surface.isEmpty() -> if (reading.isEmpty()) emptyList() else null
        reading.isEmpty() -> null
        !KanaUtils.isKanji(surface[0]) -> matchLeadingKana(surface, reading)
        else -> matchLeadingKanji(surface, reading)
    }

    private fun matchLeadingKana(surface: String, reading: String): List<FuriganaPart>? {
        val firstChar = surface[0]
        val katakana = KanaUtils.toKatakana(firstChar.toString())
        if (katakana != reading[0].toString()) return null
        val rest = match(surface.substring(1), reading.substring(1))
        return rest?.let { listOf(FuriganaPart.kana(firstChar.toString())) + it }
    }

    private fun matchLeadingKanji(surface: String, reading: String): List<FuriganaPart>? {
        val kanjiEnd = findKanjiEnd(surface)
        if (kanjiEnd == surface.length) return kanjiSplitter.split(surface, reading)
        return matchKanjiWithKanaBoundary(surface, reading, kanjiEnd)
    }

    private fun findKanjiEnd(surface: String): Int {
        var i = 1
        while (i < surface.length && KanaUtils.isKanji(surface[i])) i++
        return i
    }

    private fun matchKanjiWithKanaBoundary(
        surface: String,
        reading: String,
        kanjiEnd: Int
    ): List<FuriganaPart>? {
        val kanaChar = KanaUtils.toKatakana(surface[kanjiEnd].toString())
        var j = 1
        while (j < reading.length) {
            if (kanaChar == reading[j].toString()) {
                val rest = match(surface.substring(kanjiEnd), reading.substring(j))
                if (rest != null) {
                    val kanjiPart = surface.substring(0, kanjiEnd)
                    val kanaPart = reading.substring(0, j)
                    return kanjiSplitter.split(kanjiPart, kanaPart) + rest
                }
            }
            j++
        }
        return null
    }
}
