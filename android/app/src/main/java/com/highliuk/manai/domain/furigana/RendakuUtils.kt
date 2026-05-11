package com.highliuk.manai.domain.furigana

object RendakuUtils {

    fun rendaku(text: String): Set<String> {
        val first = text.first().toString()
        val rest = text.drop(1)
        return setOf(first, dakuten(first), handakuten(first))
            .map { it + rest }
            .toSet()
    }

    private fun dakuten(char: String): String = DAKUTEN_MAP.getOrDefault(char, char)

    private fun handakuten(char: String): String = HANDAKUTEN_MAP.getOrDefault(char, char)

    private val DAKUTEN_MAP = mapOf(
        "か" to "が", "き" to "ぎ", "く" to "ぐ", "け" to "げ", "こ" to "ご",
        "さ" to "ざ", "し" to "じ", "す" to "ず", "せ" to "ぜ", "そ" to "ぞ",
        "た" to "だ", "ち" to "ぢ", "つ" to "づ", "て" to "で", "と" to "ど",
        "は" to "ば", "ひ" to "び", "ふ" to "ぶ", "へ" to "べ", "ほ" to "ぼ",
        "カ" to "ガ", "キ" to "ギ", "ク" to "グ", "ケ" to "ゲ", "コ" to "ゴ",
        "サ" to "ザ", "シ" to "ジ", "ス" to "ズ", "セ" to "ゼ", "ソ" to "ゾ",
        "タ" to "ダ", "チ" to "ヂ", "ツ" to "ヅ", "テ" to "デ", "ト" to "ド",
        "ハ" to "バ", "ヒ" to "ビ", "フ" to "ブ", "ヘ" to "ベ", "ホ" to "ボ"
    )

    private val HANDAKUTEN_MAP = mapOf(
        "は" to "ぱ", "ひ" to "ぴ", "ふ" to "ぷ", "へ" to "ぺ", "ほ" to "ぽ",
        "ハ" to "パ", "ヒ" to "ピ", "フ" to "プ", "ヘ" to "ペ", "ホ" to "ポ"
    )
}
