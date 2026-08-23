package com.highliuk.manai.ui.chat.markdown

import com.highliuk.manai.domain.furigana.JapaneseRunSegmenter
import com.highliuk.manai.domain.model.FuriganaToken

/**
 * One drawable piece of rich text: plain or ruby-annotated, with inline style
 * flags carried over from markdown.
 */
data class RichTextPiece(
    val text: String,
    val ruby: String? = null,
    val bold: Boolean = false,
    val italic: Boolean = false,
    val code: Boolean = false,
)

/**
 * Pure planning layer between the markdown AST and the rendered spannable:
 * segments styled runs into Japanese/other pieces and attaches cached
 * furigana readings to closed Japanese runs.
 */
object RichTextPlanner {

    /**
     * Maps styled runs to drawable pieces. [furigana] returns the cached
     * tokens for a closed Japanese run, or null while it is still being
     * processed (the run then renders as plain text).
     */
    fun plan(
        runs: List<StyledRun>,
        isTail: Boolean,
        furigana: (String) -> List<FuriganaToken>?,
    ): List<RichTextPiece> = runs.flatMapIndexed { index, run ->
        val runIsTail = isTail && index == runs.lastIndex
        JapaneseRunSegmenter.segment(run.text, runIsTail).flatMap { segment ->
            val tokens = if (segment.isJapanese && segment.isClosed) {
                furigana(segment.text)
            } else {
                null
            }
            if (tokens == null) {
                listOf(RichTextPiece(segment.text, bold = run.bold, italic = run.italic, code = run.code))
            } else {
                tokens.flatMap { token -> token.parts }.map { part ->
                    RichTextPiece(
                        text = part.surface,
                        ruby = part.reading,
                        bold = run.bold,
                        italic = run.italic,
                        code = run.code,
                    )
                }
            }
        }
    }

    /** Closed Japanese run texts inside [runs], in order, without duplicates. */
    fun closedJapaneseRuns(runs: List<StyledRun>, isTail: Boolean): List<String> =
        runs.flatMapIndexed { index, run ->
            JapaneseRunSegmenter.segment(run.text, isTail && index == runs.lastIndex)
                .filter { it.isJapanese && it.isClosed }
                .map { it.text }
        }.distinct()

    /**
     * Closed Japanese run texts across a whole document. When [isComplete] is
     * false, the trailing Japanese run of the last text unit stays open and is
     * excluded — unless the last block is a table or code block, whose cells
     * always render as closed (matching the renderer, which never passes a
     * tail flag into table cells).
     */
    fun closedDocumentRuns(blocks: List<MarkdownBlock>, isComplete: Boolean): List<String> {
        val units = textUnits(blocks)
        val tailAllowed = !isComplete && canHoldOpenTail(blocks.lastOrNull())
        return units.flatMapIndexed { index, runs ->
            closedJapaneseRuns(runs, isTail = tailAllowed && index == units.lastIndex)
        }.distinct()
    }

    private fun canHoldOpenTail(block: MarkdownBlock?): Boolean = when (block) {
        is MarkdownBlock.Paragraph, is MarkdownBlock.Heading, is MarkdownBlock.ListItem -> true
        is MarkdownBlock.Table, is MarkdownBlock.CodeBlock, null -> false
    }

    /**
     * The rich-text units of a document, in reading order: one per paragraph,
     * heading and list item, one per table cell. Fenced code blocks render as
     * plain code and contribute none.
     */
    fun textUnits(blocks: List<MarkdownBlock>): List<List<StyledRun>> = blocks.flatMap { block ->
        when (block) {
            is MarkdownBlock.Paragraph -> listOf(block.inlines.toStyledRuns())
            is MarkdownBlock.Heading -> listOf(block.inlines.toStyledRuns())
            is MarkdownBlock.ListItem -> listOf(block.inlines.toStyledRuns())
            is MarkdownBlock.Table ->
                (block.header + block.rows.flatten()).map { cell -> cell.toStyledRuns() }

            is MarkdownBlock.CodeBlock -> emptyList()
        }
    }
}
