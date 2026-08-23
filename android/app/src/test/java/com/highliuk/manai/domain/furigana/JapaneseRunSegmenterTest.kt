package com.highliuk.manai.domain.furigana

import org.junit.Assert.assertEquals
import org.junit.Test

class JapaneseRunSegmenterTest {

    private fun japanese(text: String, closed: Boolean = true) =
        JapaneseRunSegment(text, isJapanese = true, isClosed = closed)

    private fun other(text: String) = JapaneseRunSegment(text, isJapanese = false)

    @Test
    fun emptyTextYieldsNoSegments() {
        assertEquals(emptyList<JapaneseRunSegment>(), JapaneseRunSegmenter.segment(""))
    }

    @Test
    fun pureAsciiIsOneNonJapaneseSegment() {
        assertEquals(listOf(other("hello!")), JapaneseRunSegmenter.segment("hello!"))
    }

    @Test
    fun asciiToKanaBoundarySplits() {
        assertEquals(
            listOf(other("abc"), japanese("かな")),
            JapaneseRunSegmenter.segment("abc かな".replace(" ", "")),
        )
    }

    @Test
    fun kanjiFollowedByAsciiPunctuationCloses() {
        assertEquals(
            listOf(japanese("漢字"), other("!")),
            JapaneseRunSegmenter.segment("漢字!"),
        )
    }

    @Test
    fun mixedLatinInsideJapaneseSentenceSplitsAroundIt() {
        assertEquals(
            listOf(japanese("これは"), other("OK"), japanese("です")),
            JapaneseRunSegmenter.segment("これはOKです"),
        )
    }

    @Test
    fun fullWidthMarksBetweenJapaneseStayInsideTheRun() {
        assertEquals(
            listOf(japanese("今日は。明日")),
            JapaneseRunSegmenter.segment("今日は。明日"),
        )
    }

    @Test
    fun trailingFullWidthMarkIsNotPartOfTheRun() {
        assertEquals(
            listOf(japanese("今日は"), other("。")),
            JapaneseRunSegmenter.segment("今日は。"),
        )
    }

    @Test
    fun leadingFullWidthBracketIsNotPartOfTheRun() {
        assertEquals(
            listOf(other("「"), japanese("引用"), other("」")),
            JapaneseRunSegmenter.segment("「引用」"),
        )
    }

    @Test
    fun iterationMarksAndProlongedSoundMarkAreRunInternal() {
        assertEquals(
            listOf(japanese("人々"), other(" "), japanese("スーパー")),
            JapaneseRunSegmenter.segment("人々 スーパー"),
        )
    }

    @Test
    fun katakanaMiddleDotStaysInsideTheRun() {
        assertEquals(
            listOf(japanese("マンガ・アニメ")),
            JapaneseRunSegmenter.segment("マンガ・アニメ"),
        )
    }

    @Test
    fun historyTextIsFullyClosedEvenWhenEndingInJapanese() {
        assertEquals(
            listOf(other("abc"), japanese("漢字", closed = true)),
            JapaneseRunSegmenter.segment("abc漢字", isTail = false),
        )
    }

    @Test
    fun streamingTailEndingInJapaneseStaysOpen() {
        assertEquals(
            listOf(other("abc"), japanese("漢字", closed = false)),
            JapaneseRunSegmenter.segment("abc漢字", isTail = true),
        )
    }

    @Test
    fun streamingTailWithPendingFullWidthMarkStaysOpen() {
        assertEquals(
            listOf(japanese("今日は", closed = false), other("。")),
            JapaneseRunSegmenter.segment("今日は。", isTail = true),
        )
    }

    @Test
    fun streamingRunFollowedByAsciiIsClosedEvenInTail() {
        assertEquals(
            listOf(japanese("漢字", closed = true), other("A")),
            JapaneseRunSegmenter.segment("漢字A", isTail = true),
        )
    }

    @Test
    fun earlierRunsAreClosedEvenWhenTailIsOpen() {
        assertEquals(
            listOf(
                japanese("最初", closed = true),
                other("A"),
                japanese("最後", closed = false),
            ),
            JapaneseRunSegmenter.segment("最初A最後", isTail = true),
        )
    }

    @Test
    fun halfWidthKatakanaDeliberatelyClassifiesAsNonJapanese() {
        // Half-width katakana (U+FF66..U+FF9D) gets no furigana: the
        // tokenizer dictionary targets full-width script, so these runs
        // stay plain by design.
        assertEquals(
            listOf(japanese("漢字"), other("ｶﾀｶﾅ!")),
            JapaneseRunSegmenter.segment("漢字ｶﾀｶﾅ!"),
        )
    }

    @Test
    fun segmentsConcatenateBackToOriginalText() {
        val text = "「今日は。」と**言った**JLPT N5、すごい！ね"
        val rebuilt = JapaneseRunSegmenter.segment(text, isTail = true)
            .joinToString("") { it.text }

        assertEquals(text, rebuilt)
    }
}
