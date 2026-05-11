package com.highliuk.manai.domain.ml

interface JapaneseTokenizer {
    suspend fun init()
    fun tokenize(text: String): List<TokenizerResult>
}
