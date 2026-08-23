package com.highliuk.manai.ui.chat.markdown

/** Inline content of a markdown block: styled runs of text. */
sealed interface MarkdownInline {
    data class Text(val text: String) : MarkdownInline
    data class Bold(val children: List<MarkdownInline>) : MarkdownInline
    data class Italic(val children: List<MarkdownInline>) : MarkdownInline
    data class Code(val text: String) : MarkdownInline
}

/** A flattened inline run with resolved styling, ready for rendering. */
data class StyledRun(
    val text: String,
    val bold: Boolean = false,
    val italic: Boolean = false,
    val code: Boolean = false,
)

/** Flattens a nested inline tree into a list of styled runs. */
fun List<MarkdownInline>.toStyledRuns(
    bold: Boolean = false,
    italic: Boolean = false,
): List<StyledRun> = flatMap { inline ->
    when (inline) {
        is MarkdownInline.Text -> listOf(StyledRun(inline.text, bold = bold, italic = italic))
        is MarkdownInline.Code -> listOf(
            StyledRun(inline.text, bold = bold, italic = italic, code = true)
        )
        is MarkdownInline.Bold -> inline.children.toStyledRuns(bold = true, italic = italic)
        is MarkdownInline.Italic -> inline.children.toStyledRuns(bold = bold, italic = true)
    }
}

/** Block-level markdown structure covering the subset the tutor prompts emit. */
sealed interface MarkdownBlock {
    data class Paragraph(val inlines: List<MarkdownInline>) : MarkdownBlock

    data class Heading(val level: Int, val inlines: List<MarkdownInline>) : MarkdownBlock

    data class ListItem(
        val ordered: Boolean,
        val index: Int,
        val level: Int,
        val inlines: List<MarkdownInline>,
    ) : MarkdownBlock

    data class Table(
        val header: List<List<MarkdownInline>>,
        val rows: List<List<List<MarkdownInline>>>,
    ) : MarkdownBlock

    data class CodeBlock(val text: String) : MarkdownBlock
}
