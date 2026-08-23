package com.highliuk.manai.ui.chat.markdown

/**
 * Minimal dependency-free markdown parser for the subset the tutor prompts
 * produce: paragraphs, ATX headings, ordered/unordered lists, pipe tables,
 * fenced code blocks and inline bold/italic/code. Malformed input degrades
 * to plain text and never throws.
 */
object MarkdownParser {

    private const val FENCE = "```"
    private const val INDENT_PER_LEVEL = 2

    private val headingRegex = Regex("""^(#{1,6})\s+(.*)$""")
    private val listItemRegex = Regex("""^(\s*)(?:([-+*])|(\d{1,9})[.)])\s+(.*)$""")
    private val tableSeparatorRegex = Regex("""^\|?\s*:?-+:?\s*(\|\s*:?-+:?\s*)*\|?$""")

    fun parse(markdown: String): List<MarkdownBlock> {
        val lines = markdown.replace("\r\n", "\n").replace('\r', '\n').split("\n")
        val blocks = mutableListOf<MarkdownBlock>()
        val paragraph = mutableListOf<String>()
        var i = 0
        while (i < lines.size) {
            val line = lines[i]
            i = when {
                line.trim().startsWith(FENCE) -> {
                    flushParagraph(paragraph, blocks)
                    consumeCodeBlock(lines, i + 1, blocks)
                }

                line.isBlank() -> {
                    flushParagraph(paragraph, blocks)
                    i + 1
                }

                isTableStart(lines, i) -> {
                    flushParagraph(paragraph, blocks)
                    consumeTable(lines, i, blocks)
                }

                parseLineBlock(line, blocks, paragraph) -> i + 1

                else -> {
                    paragraph.add(line)
                    i + 1
                }
            }
        }
        flushParagraph(paragraph, blocks)
        return blocks
    }

    fun parseInlines(text: String): List<MarkdownInline> = MarkdownInlineParser.parse(text)

    /** Parses [line] as heading or list item; returns false when it is neither. */
    private fun parseLineBlock(
        line: String,
        blocks: MutableList<MarkdownBlock>,
        paragraph: MutableList<String>,
    ): Boolean {
        val block = parseHeading(line) ?: parseListItem(line)
        if (block != null) {
            flushParagraph(paragraph, blocks)
            blocks.add(block)
        }
        return block != null
    }

    private fun parseHeading(line: String): MarkdownBlock? =
        headingRegex.matchEntire(line)?.let { match ->
            val (hashes, content) = match.destructured
            MarkdownBlock.Heading(hashes.length, parseInlines(content.trim()))
        }

    private fun parseListItem(line: String): MarkdownBlock? =
        listItemRegex.matchEntire(line)?.let { match ->
            val number = match.groupValues[3]
            MarkdownBlock.ListItem(
                ordered = number.isNotEmpty(),
                index = number.toIntOrNull() ?: 0,
                level = match.groupValues[1].length / INDENT_PER_LEVEL,
                inlines = parseInlines(match.groupValues[4]),
            )
        }

    private fun flushParagraph(paragraph: MutableList<String>, blocks: MutableList<MarkdownBlock>) {
        if (paragraph.isEmpty()) return
        blocks.add(MarkdownBlock.Paragraph(parseInlines(paragraph.joinToString("\n"))))
        paragraph.clear()
    }

    private fun consumeCodeBlock(
        lines: List<String>,
        start: Int,
        blocks: MutableList<MarkdownBlock>,
    ): Int {
        val body = mutableListOf<String>()
        var i = start
        while (i < lines.size && !lines[i].trim().startsWith(FENCE)) {
            body.add(lines[i])
            i++
        }
        blocks.add(MarkdownBlock.CodeBlock(body.joinToString("\n")))
        return if (i < lines.size) i + 1 else i
    }

    private fun isTableStart(lines: List<String>, i: Int): Boolean =
        lines[i].trim().startsWith("|") &&
            i + 1 < lines.size &&
            tableSeparatorRegex.matches(lines[i + 1].trim()) &&
            lines[i + 1].contains("-")

    private fun consumeTable(
        lines: List<String>,
        start: Int,
        blocks: MutableList<MarkdownBlock>,
    ): Int {
        val header = parseTableRow(lines[start])
        val rows = mutableListOf<List<List<MarkdownInline>>>()
        var i = start + 2
        while (i < lines.size && lines[i].trim().startsWith("|")) {
            rows.add(parseTableRow(lines[i]))
            i++
        }
        blocks.add(MarkdownBlock.Table(header, rows))
        return i
    }

    private fun parseTableRow(line: String): List<List<MarkdownInline>> = line.trim()
        .removePrefix("|")
        .removeSuffix("|")
        .split("|")
        .map { cell -> parseInlines(cell.trim()) }
}
