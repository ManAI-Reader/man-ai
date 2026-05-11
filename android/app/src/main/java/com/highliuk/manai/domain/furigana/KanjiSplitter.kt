package com.highliuk.manai.domain.furigana

import com.highliuk.manai.domain.model.FuriganaPart

class KanjiSplitter(private val readingsDataSource: KanjiReadingsDataSource) {

    fun split(kanji: String, kana: String): List<FuriganaPart> {
        val hiragana = KanaUtils.toHiragana(kana)
        val fallback = listOf(FuriganaPart.kanji(kanji, hiragana))

        if (kanji.length == 1) return fallback

        return splitMultiKanji(kanji, kana) ?: fallback
    }

    private fun splitMultiKanji(kanji: String, kana: String): List<FuriganaPart>? {
        val kanjiChars = kanji.toList()
        val perKanjiReadings = buildReadingsPerKanji(kanjiChars) ?: return null

        val match = combine(perKanjiReadings).firstOrNull { combination ->
            combination.joinToString("") { KanaUtils.toKatakana(it) } == kana
        }

        return match?.let { combo ->
            kanjiChars.zip(combo).map { (k, r) ->
                FuriganaPart.kanji(k.toString(), KanaUtils.toHiragana(r))
            }
        }
    }

    private fun buildReadingsPerKanji(kanjiChars: List<Char>): List<List<String>>? {
        val result = kanjiChars.mapIndexed { index, char ->
            val base = readingsDataSource.getReadings(char)
            if (base.isEmpty()) return null
            if (index == 0) base else base.flatMap { RendakuUtils.rendaku(it) }
        }
        return result
    }
}

private fun <T> combine(lists: List<List<T>>): List<List<T>> {
    if (lists.isEmpty()) return listOf(emptyList())
    val first = lists.first()
    val rest = combine(lists.drop(1))
    return first.flatMap { item -> rest.map { combination -> listOf(item) + combination } }
}
