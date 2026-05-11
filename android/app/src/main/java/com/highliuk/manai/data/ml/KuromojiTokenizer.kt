package com.highliuk.manai.data.ml

import com.atilika.kuromoji.ipadic.Tokenizer
import com.highliuk.manai.domain.ml.JapaneseTokenizer
import com.highliuk.manai.domain.ml.TokenizerResult
import javax.inject.Inject

class KuromojiTokenizer @Inject constructor() : JapaneseTokenizer {

    private lateinit var tokenizer: Tokenizer

    override suspend fun init() {
        if (::tokenizer.isInitialized) return
        tokenizer = Tokenizer()
    }

    override fun tokenize(text: String): List<TokenizerResult> =
        tokenizer.tokenize(text).map { token ->
            TokenizerResult(
                surface = token.surface,
                reading = token.reading.takeIf { it != "*" }
            )
        }
}
