package com.highliuk.manai.domain.furigana

/**
 * A contiguous piece of text classified for furigana processing.
 *
 * @property isJapanese whether the piece is a run of Japanese characters.
 * @property isClosed whether the run is finalized: an open run is the trailing
 * Japanese run of a still-streaming text, which may still grow and must not be
 * furigana-processed yet.
 */
data class JapaneseRunSegment(
    val text: String,
    val isJapanese: Boolean,
    val isClosed: Boolean = true,
)

/**
 * Splits text into runs of Japanese characters (kanji, kana, iteration marks,
 * prolonged sound mark) and everything else. Full-width punctuation joins two
 * Japanese parts into a single run but never starts or ends one.
 */
object JapaneseRunSegmenter {

    fun segment(text: String, isTail: Boolean = false): List<JapaneseRunSegment> {
        val segments = mutableListOf<JapaneseRunSegment>()
        var i = 0
        while (i < text.length) {
            i = if (isCore(text[i])) {
                consumeJapaneseRun(text, i, isTail, segments)
            } else {
                consumeOtherRun(text, i, segments)
            }
        }
        return segments
    }

    private fun isCore(char: Char): Boolean = KanaUtils.isJapanese(char)

    private fun isFullWidthMark(char: Char): Boolean =
        char in CJK_PUNCTUATION || char in FULL_WIDTH_FORMS

    /** Consumes a Japanese run starting at [start]; returns the exclusive end index. */
    private fun consumeJapaneseRun(
        text: String,
        start: Int,
        isTail: Boolean,
        segments: MutableList<JapaneseRunSegment>,
    ): Int {
        var end = start + 1
        var i = start + 1
        while (i < text.length) {
            if (isCore(text[i])) {
                i++
                end = i
            } else {
                val next = skipInternalMarks(text, i) ?: break
                i = next
            }
        }
        val followedOnlyByMarks = (end until text.length).all { isFullWidthMark(text[it]) }
        segments.add(
            JapaneseRunSegment(
                text = text.substring(start, end),
                isJapanese = true,
                isClosed = !(isTail && followedOnlyByMarks),
            ),
        )
        return end
    }

    /**
     * Returns the index of the Japanese character that follows a sequence of
     * full-width marks starting at [from], or null when the marks are not
     * followed by more Japanese text (so they are not run-internal).
     */
    private fun skipInternalMarks(text: String, from: Int): Int? {
        if (!isFullWidthMark(text[from])) return null
        var j = from
        while (j < text.length && isFullWidthMark(text[j])) {
            j++
        }
        return if (j < text.length && isCore(text[j])) j else null
    }

    private fun consumeOtherRun(
        text: String,
        start: Int,
        segments: MutableList<JapaneseRunSegment>,
    ): Int {
        var i = start + 1
        while (i < text.length && !isCore(text[i])) {
            i++
        }
        segments.add(JapaneseRunSegment(text.substring(start, i), isJapanese = false))
        return i
    }

    private val CJK_PUNCTUATION = '　'..'〿'

    // U+FF00..U+FF60 covers full-width punctuation only. Half-width katakana
    // (U+FF66..U+FF9D) deliberately stays outside: the tokenizer dictionary
    // targets full-width script, so half-width runs classify as non-Japanese
    // and receive no furigana.
    private val FULL_WIDTH_FORMS = '＀'..'｠'
}
