package com.highliuk.manai.domain.model

data class FuriganaPart(
    val surface: String,
    val reading: String?
) {
    companion object {
        fun kanji(surface: String, reading: String): FuriganaPart =
            FuriganaPart(surface, if (surface == reading) null else reading)

        fun kana(surface: String): FuriganaPart =
            FuriganaPart(surface, null)
    }
}
