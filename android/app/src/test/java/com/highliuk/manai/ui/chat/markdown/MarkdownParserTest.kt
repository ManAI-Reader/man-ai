package com.highliuk.manai.ui.chat.markdown

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkdownParserTest {

    private fun text(value: String) = MarkdownInline.Text(value)

    // region paragraphs

    @Test
    fun plainTextBecomesSingleParagraph() {
        val blocks = MarkdownParser.parse("Hello world")

        assertEquals(listOf(MarkdownBlock.Paragraph(listOf(text("Hello world")))), blocks)
    }

    @Test
    fun blankLineSeparatesParagraphs() {
        val blocks = MarkdownParser.parse("First\n\nSecond")

        assertEquals(
            listOf(
                MarkdownBlock.Paragraph(listOf(text("First"))),
                MarkdownBlock.Paragraph(listOf(text("Second"))),
            ),
            blocks,
        )
    }

    @Test
    fun consecutiveLinesJoinIntoOneParagraphKeepingLineBreaks() {
        val blocks = MarkdownParser.parse("line one\nline two")

        assertEquals(
            listOf(MarkdownBlock.Paragraph(listOf(text("line one\nline two")))),
            blocks,
        )
    }

    @Test
    fun crlfInputParsesLikeLfInput() {
        val crlf = MarkdownParser.parse("First\r\n\r\nSecond\r\n")

        assertEquals(MarkdownParser.parse("First\n\nSecond\n"), crlf)
    }

    // endregion

    // region headings

    @Test
    fun headingLevelsAreParsed() {
        val blocks = MarkdownParser.parse("# One\n### Three")

        assertEquals(
            listOf(
                MarkdownBlock.Heading(1, listOf(text("One"))),
                MarkdownBlock.Heading(3, listOf(text("Three"))),
            ),
            blocks,
        )
    }

    @Test
    fun hashesWithoutSpaceAreNotHeadings() {
        val blocks = MarkdownParser.parse("#tag")

        assertEquals(listOf(MarkdownBlock.Paragraph(listOf(text("#tag")))), blocks)
    }

    // endregion

    // region inline styles

    @Test
    fun boldInlineIsParsed() {
        val inlines = MarkdownParser.parseInlines("a **b** c")

        assertEquals(
            listOf(text("a "), MarkdownInline.Bold(listOf(text("b"))), text(" c")),
            inlines,
        )
    }

    @Test
    fun italicInlineIsParsed() {
        val inlines = MarkdownParser.parseInlines("a *b* c")

        assertEquals(
            listOf(text("a "), MarkdownInline.Italic(listOf(text("b"))), text(" c")),
            inlines,
        )
    }

    @Test
    fun inlineCodeIsParsedAndMarkersInsideAreLiteral() {
        val inlines = MarkdownParser.parseInlines("use `**not bold**` here")

        assertEquals(
            listOf(text("use "), MarkdownInline.Code("**not bold**"), text(" here")),
            inlines,
        )
    }

    @Test
    fun italicInsideBoldIsNested() {
        val inlines = MarkdownParser.parseInlines("**a *b***")

        assertEquals(
            listOf(
                MarkdownInline.Bold(
                    listOf(text("a "), MarkdownInline.Italic(listOf(text("b")))),
                ),
            ),
            inlines,
        )
    }

    @Test
    fun unclosedBoldDegradesToLiteralText() {
        val inlines = MarkdownParser.parseInlines("a **b")

        assertEquals(listOf(text("a **b")), inlines)
    }

    @Test
    fun unclosedCodeDegradesToLiteralText() {
        val inlines = MarkdownParser.parseInlines("a `b")

        assertEquals(listOf(text("a `b")), inlines)
    }

    @Test
    fun asteriskFollowedBySpaceIsNotAnEmphasisOpener() {
        val inlines = MarkdownParser.parseInlines("2 * 3 * 4")

        assertEquals(listOf(text("2 * 3 * 4")), inlines)
    }

    @Test
    fun boldAroundJapaneseKeepsTextIntact() {
        val inlines = MarkdownParser.parseInlines("**強調**する")

        assertEquals(
            listOf(MarkdownInline.Bold(listOf(text("強調"))), text("する")),
            inlines,
        )
    }

    // endregion

    // region lists

    @Test
    fun unorderedListItemsAreParsed() {
        val blocks = MarkdownParser.parse("- one\n- two")

        assertEquals(
            listOf(
                MarkdownBlock.ListItem(ordered = false, index = 0, level = 0, listOf(text("one"))),
                MarkdownBlock.ListItem(ordered = false, index = 0, level = 0, listOf(text("two"))),
            ),
            blocks,
        )
    }

    @Test
    fun orderedListItemsKeepTheirNumbers() {
        val blocks = MarkdownParser.parse("1. first\n2. second")

        assertEquals(
            listOf(
                MarkdownBlock.ListItem(ordered = true, index = 1, level = 0, listOf(text("first"))),
                MarkdownBlock.ListItem(ordered = true, index = 2, level = 0, listOf(text("second"))),
            ),
            blocks,
        )
    }

    @Test
    fun indentedListItemsGetDeeperLevels() {
        val blocks = MarkdownParser.parse("- top\n  - nested")

        assertEquals(
            listOf(
                MarkdownBlock.ListItem(ordered = false, index = 0, level = 0, listOf(text("top"))),
                MarkdownBlock.ListItem(
                    ordered = false, index = 0, level = 1, listOf(text("nested")),
                ),
            ),
            blocks,
        )
    }

    @Test
    fun boldInsideListItemIsNested() {
        val blocks = MarkdownParser.parse("1. **意味** — base meaning")

        assertEquals(
            listOf(
                MarkdownBlock.ListItem(
                    ordered = true,
                    index = 1,
                    level = 0,
                    inlines = listOf(
                        MarkdownInline.Bold(listOf(text("意味"))),
                        text(" — base meaning"),
                    ),
                ),
            ),
            blocks,
        )
    }

    // endregion

    // region tables

    @Test
    fun tableWithHeaderAndRowsIsParsed() {
        val blocks = MarkdownParser.parse("| A | B |\n|---|---|\n| 1 | 2 |")

        assertEquals(
            listOf(
                MarkdownBlock.Table(
                    header = listOf(listOf(text("A")), listOf(text("B"))),
                    rows = listOf(listOf(listOf(text("1")), listOf(text("2")))),
                ),
            ),
            blocks,
        )
    }

    @Test
    fun tableWithIrregularRowWidthsIsTolerated() {
        val blocks = MarkdownParser.parse("| A | B |\n|---|---|\n| 1 |\n| 1 | 2 | 3 |")

        val table = blocks.single() as MarkdownBlock.Table
        assertEquals(2, table.header.size)
        assertEquals(listOf(1, 3), table.rows.map { it.size })
    }

    @Test
    fun pipeLineWithoutSeparatorStaysAParagraph() {
        val blocks = MarkdownParser.parse("| just | text |")

        assertTrue(blocks.single() is MarkdownBlock.Paragraph)
    }

    // endregion

    // region code blocks

    @Test
    fun fencedCodeBlockIsParsed() {
        val blocks = MarkdownParser.parse("```\nval x = 1\n```")

        assertEquals(listOf(MarkdownBlock.CodeBlock("val x = 1")), blocks)
    }

    @Test
    fun unterminatedCodeBlockConsumesRestOfInput() {
        val blocks = MarkdownParser.parse("```\nval x = 1")

        assertEquals(listOf(MarkdownBlock.CodeBlock("val x = 1")), blocks)
    }

    // endregion

    // region horizontal rules

    @Test
    fun dashesAloneOnALineBecomeAHorizontalRule() {
        val blocks = MarkdownParser.parse("---")

        assertEquals(listOf(MarkdownBlock.HorizontalRule), blocks)
    }

    @Test
    fun asterisksUnderscoresAndLongerMarkersAlsoFormHorizontalRules() {
        assertEquals(listOf(MarkdownBlock.HorizontalRule), MarkdownParser.parse("***"))
        assertEquals(listOf(MarkdownBlock.HorizontalRule), MarkdownParser.parse("___"))
        assertEquals(listOf(MarkdownBlock.HorizontalRule), MarkdownParser.parse("-----"))
    }

    @Test
    fun horizontalRuleBetweenParagraphsSplitsThem() {
        val blocks = MarkdownParser.parse("First\n\n---\n\nSecond")

        assertEquals(
            listOf(
                MarkdownBlock.Paragraph(listOf(text("First"))),
                MarkdownBlock.HorizontalRule,
                MarkdownBlock.Paragraph(listOf(text("Second"))),
            ),
            blocks,
        )
    }

    @Test
    fun horizontalRuleDirectlyUnderAParagraphLineStillSplits() {
        val blocks = MarkdownParser.parse("First\n---\nSecond")

        assertEquals(
            listOf(
                MarkdownBlock.Paragraph(listOf(text("First"))),
                MarkdownBlock.HorizontalRule,
                MarkdownBlock.Paragraph(listOf(text("Second"))),
            ),
            blocks,
        )
    }

    @Test
    fun dashesInsideASentenceAreNotARule() {
        val blocks = MarkdownParser.parse("please --- not a rule")

        assertEquals(
            listOf(MarkdownBlock.Paragraph(listOf(text("please --- not a rule")))),
            blocks,
        )
    }

    @Test
    fun fewerThanThreeMarkerCharsIsNotARule() {
        val blocks = MarkdownParser.parse("--")

        assertEquals(listOf(MarkdownBlock.Paragraph(listOf(text("--")))), blocks)
    }

    // endregion

    // region blockquotes

    @Test
    fun quotedLineBecomesABlockquote() {
        val blocks = MarkdownParser.parse("> quoted")

        assertEquals(listOf(MarkdownBlock.Blockquote(listOf(text("quoted")))), blocks)
    }

    @Test
    fun consecutiveQuoteLinesGroupIntoOneBlockquote() {
        val blocks = MarkdownParser.parse("> one\n> two")

        assertEquals(listOf(MarkdownBlock.Blockquote(listOf(text("one\ntwo")))), blocks)
    }

    @Test
    fun blockquoteSupportsInlineStyles() {
        val blocks = MarkdownParser.parse("> **bold** words")

        assertEquals(
            listOf(
                MarkdownBlock.Blockquote(
                    listOf(MarkdownInline.Bold(listOf(text("bold"))), text(" words")),
                ),
            ),
            blocks,
        )
    }

    @Test
    fun quoteMarkerWithoutSpaceIsStillAQuote() {
        val blocks = MarkdownParser.parse(">quoted")

        assertEquals(listOf(MarkdownBlock.Blockquote(listOf(text("quoted")))), blocks)
    }

    @Test
    fun blockquoteEndsAtTheFirstNonQuoteLine() {
        val blocks = MarkdownParser.parse("> quoted\nplain")

        assertEquals(
            listOf(
                MarkdownBlock.Blockquote(listOf(text("quoted"))),
                MarkdownBlock.Paragraph(listOf(text("plain"))),
            ),
            blocks,
        )
    }

    // endregion

    // region robustness

    @Test
    fun garbageMarkerSoupNeverCrashes() {
        val soup = "*** ** * ` ``` | |- # ###x\n\n**`*`**\n|---|\n``"

        val blocks = MarkdownParser.parse(soup)

        assertTrue(blocks.isNotEmpty())
    }

    @Test
    fun emptyInputYieldsNoBlocks() {
        assertEquals(emptyList<MarkdownBlock>(), MarkdownParser.parse(""))
    }

    @Test
    fun spansAtTheDepthCapDegradeToLiteralText() {
        val inlines = MarkdownInlineParser.parse("**bold**", depth = MarkdownInlineParser.MAX_DEPTH)

        assertEquals(listOf(text("**bold**")), inlines)
    }

    @Test
    fun spansJustBelowTheDepthCapStillParse() {
        val inlines = MarkdownInlineParser.parse(
            "**bold**",
            depth = MarkdownInlineParser.MAX_DEPTH - 1,
        )

        assertEquals(listOf(MarkdownInline.Bold(listOf(text("bold")))), inlines)
    }

    // endregion

    // region styled runs

    @Test
    fun styledRunsFlattenNestedStyles() {
        val runs = MarkdownParser.parseInlines("**a *b*** `c`").toStyledRuns()

        assertEquals(
            listOf(
                StyledRun("a ", bold = true),
                StyledRun("b", bold = true, italic = true),
                StyledRun(" "),
                StyledRun("c", code = true),
            ),
            runs,
        )
    }

    // endregion
}
