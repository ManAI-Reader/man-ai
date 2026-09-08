package com.highliuk.manai.ui.chat.markdown

/**
 * Parses inline markdown spans (bold, italic, inline code) into
 * [MarkdownInline] nodes. Unclosed markers degrade to literal text.
 */
internal object MarkdownInlineParser {

    private const val BOLD_MARKER = "**"
    private const val ITALIC_MARKER = "*"
    private const val CODE_MARKER = "`"

    /** Nesting cap: content deeper than this degrades to literal text. */
    const val MAX_DEPTH = 16

    fun parse(text: String, depth: Int = 0): List<MarkdownInline> {
        if (depth >= MAX_DEPTH) return listOf(MarkdownInline.Text(text))
        val result = mutableListOf<MarkdownInline>()
        val literal = StringBuilder()
        var i = 0
        while (i < text.length) {
            val consumed = tryConsumeSpan(text, i, literal, result, depth)
            if (consumed > 0) {
                i += consumed
            } else {
                literal.append(text[i])
                i++
            }
        }
        flushLiteral(literal, result)
        return result
    }

    /**
     * Attempts to consume an inline span (code, bold or italic) starting at
     * [start]. Returns the number of consumed characters, or 0 when no valid
     * span starts there (the caller then treats the character as literal).
     */
    private fun tryConsumeSpan(
        text: String,
        start: Int,
        literal: StringBuilder,
        result: MutableList<MarkdownInline>,
        depth: Int,
    ): Int = when {
        text.startsWith(CODE_MARKER, start) -> consumeCode(text, start, literal, result)
        text.startsWith(BOLD_MARKER, start) -> consumeBold(SpanContext(text, start, depth), literal, result)
        text.startsWith(ITALIC_MARKER, start) -> consumeItalic(SpanContext(text, start, depth), literal, result)
        else -> 0
    }

    /** Where an inline span candidate starts and how deep we already are. */
    private data class SpanContext(val text: String, val start: Int, val depth: Int)

    private fun consumeCode(
        text: String,
        start: Int,
        literal: StringBuilder,
        result: MutableList<MarkdownInline>,
    ): Int {
        val contentStart = start + CODE_MARKER.length
        val end = text.indexOf(CODE_MARKER, contentStart)
        if (end <= contentStart) return 0
        flushLiteral(literal, result)
        result.add(MarkdownInline.Code(text.substring(contentStart, end)))
        return end + CODE_MARKER.length - start
    }

    private fun consumeBold(
        context: SpanContext,
        literal: StringBuilder,
        result: MutableList<MarkdownInline>,
    ): Int {
        val (text, start, depth) = context
        val contentStart = start + BOLD_MARKER.length
        var end = text.indexOf(BOLD_MARKER, contentStart)
        // No emphasis when unclosed, empty, or opened right before whitespace
        // (so "2 * 3 * 4" stays literal).
        if (end <= contentStart || text[contentStart].isWhitespace()) return 0
        // Prefer the rightmost closer in a run of asterisks so that
        // "**a *b***" closes the bold after the inner italic.
        while (end + BOLD_MARKER.length < text.length && text[end + BOLD_MARKER.length] == '*') {
            end++
        }
        flushLiteral(literal, result)
        result.add(MarkdownInline.Bold(parse(text.substring(contentStart, end), depth + 1)))
        return end + BOLD_MARKER.length - start
    }

    private fun consumeItalic(
        context: SpanContext,
        literal: StringBuilder,
        result: MutableList<MarkdownInline>,
    ): Int {
        val (text, start, depth) = context
        val contentStart = start + ITALIC_MARKER.length
        val end = text.indexOf(ITALIC_MARKER, contentStart)
        if (end <= contentStart || text[contentStart].isWhitespace()) return 0
        flushLiteral(literal, result)
        result.add(MarkdownInline.Italic(parse(text.substring(contentStart, end), depth + 1)))
        return end + ITALIC_MARKER.length - start
    }

    private fun flushLiteral(literal: StringBuilder, result: MutableList<MarkdownInline>) {
        if (literal.isEmpty()) return
        result.add(MarkdownInline.Text(literal.toString()))
        literal.clear()
    }
}
