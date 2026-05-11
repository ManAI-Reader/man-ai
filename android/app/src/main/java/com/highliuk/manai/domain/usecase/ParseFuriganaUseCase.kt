package com.highliuk.manai.domain.usecase

import com.highliuk.manai.domain.furigana.FuriganaMatcher
import com.highliuk.manai.domain.furigana.KanaUtils
import com.highliuk.manai.domain.ml.JapaneseTokenizer
import com.highliuk.manai.domain.ml.TokenizerResult
import com.highliuk.manai.domain.model.FuriganaPart
import com.highliuk.manai.domain.model.FuriganaToken
import javax.inject.Inject

class ParseFuriganaUseCase @Inject constructor(
    private val tokenizer: JapaneseTokenizer,
    private val matcher: FuriganaMatcher,
) {
    operator fun invoke(text: String): List<FuriganaToken> {
        if (text.isEmpty()) return emptyList()

        return splitTextIntoParts(text).flatMap { segment ->
            if (segment.any { KanaUtils.isJapanese(it) }) {
                parseJapaneseSegment(segment)
            } else {
                listOf(passthroughToken(segment))
            }
        }
    }

    private fun parseJapaneseSegment(text: String): List<FuriganaToken> =
        tokenizer.tokenize(text).map { result -> buildToken(result) }

    private fun buildToken(result: TokenizerResult): FuriganaToken {
        val parts = getPartsFromToken(result)
        return FuriganaToken(surface = result.surface, reading = result.reading, parts = parts)
    }

    private fun getPartsFromToken(token: TokenizerResult): List<FuriganaPart> {
        val surface = token.surface
        val reading = token.reading
        val fallback = listOf(FuriganaPart.kana(surface))

        if (reading == null || !KanaUtils.isKatakana(reading) || !surface.all { KanaUtils.isJapanese(it) }) {
            return fallback
        }

        return matcher.match(surface, reading) ?: fallback
    }

    private fun passthroughToken(text: String): FuriganaToken =
        FuriganaToken(surface = text, reading = null, parts = listOf(FuriganaPart.kana(text)))
}

private fun splitTextIntoParts(text: String): List<String> {
    if (text.length <= 1) return listOf(text)

    val result = mutableListOf<String>()
    val current = StringBuilder()
    current.append(text[0])
    var currentIsJapanese = KanaUtils.isJapanese(text[0])

    for (i in 1 until text.length) {
        val charIsJapanese = KanaUtils.isJapanese(text[i])
        if (charIsJapanese != currentIsJapanese) {
            result.add(current.toString())
            current.clear()
            currentIsJapanese = charIsJapanese
        }
        current.append(text[i])
    }
    result.add(current.toString())
    return result
}
