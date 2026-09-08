package com.highliuk.manai.ui.chat.markdown

import com.highliuk.manai.domain.model.FuriganaPart
import com.highliuk.manai.domain.model.FuriganaToken
import org.junit.Assert.assertEquals
import org.junit.Test

class RichTextPlannerTest {

    private val kanjiToken = FuriganaToken(
        surface = "漢字",
        reading = "カンジ",
        parts = listOf(FuriganaPart.kanji("漢字", "かんじ")),
    )

    private val mixedToken = FuriganaToken(
        surface = "食べる",
        reading = "タベル",
        parts = listOf(FuriganaPart.kanji("食", "た"), FuriganaPart.kana("べる")),
    )

    private fun noFurigana(text: String): List<FuriganaToken>? {
        require(text.isNotEmpty())
        return null
    }

    // region plan

    @Test
    fun plainAsciiRunYieldsSinglePlainPiece() {
        val pieces = RichTextPlanner.plan(
            runs = listOf(StyledRun("hello")),
            isTail = false,
            furigana = ::noFurigana,
        )

        assertEquals(listOf(RichTextPiece("hello")), pieces)
    }

    @Test
    fun closedJapaneseRunWithTokensYieldsRubyPieces() {
        val pieces = RichTextPlanner.plan(
            runs = listOf(StyledRun("食べる")),
            isTail = false,
            furigana = { listOf(mixedToken) },
        )

        assertEquals(
            listOf(RichTextPiece("食", ruby = "た"), RichTextPiece("べる")),
            pieces,
        )
    }

    @Test
    fun unresolvedJapaneseRunFallsBackToPlainText() {
        val pieces = RichTextPlanner.plan(
            runs = listOf(StyledRun("漢字")),
            isTail = false,
            furigana = ::noFurigana,
        )

        assertEquals(listOf(RichTextPiece("漢字")), pieces)
    }

    @Test
    fun openTailRunIsNeverLookedUp() {
        val lookedUp = mutableListOf<String>()
        val pieces = RichTextPlanner.plan(
            runs = listOf(StyledRun("abc漢字")),
            isTail = true,
            furigana = { text ->
                lookedUp.add(text)
                null
            },
        )

        assertEquals(listOf(RichTextPiece("abc"), RichTextPiece("漢字")), pieces)
        assertEquals(emptyList<String>(), lookedUp)
    }

    @Test
    fun onlyLastRunOfTailUnitCanStayOpen() {
        val lookedUp = mutableListOf<String>()
        RichTextPlanner.plan(
            runs = listOf(StyledRun("漢字", bold = true), StyledRun("かな")),
            isTail = true,
            furigana = { text ->
                lookedUp.add(text)
                null
            },
        )

        assertEquals(listOf("漢字"), lookedUp)
    }

    @Test
    fun inlineStylesPropagateToRubyPieces() {
        val pieces = RichTextPlanner.plan(
            runs = listOf(StyledRun("漢字", bold = true, code = true)),
            isTail = false,
            furigana = { listOf(kanjiToken) },
        )

        assertEquals(
            listOf(RichTextPiece("漢字", ruby = "かんじ", bold = true, code = true)),
            pieces,
        )
    }

    @Test
    fun markdownStrippedBoldJapaneseIsOneRunPerStyledSpan() {
        val runs = MarkdownParser.parseInlines("**強調**する").toStyledRuns()

        val pieces = RichTextPlanner.plan(
            runs = runs,
            isTail = false,
            furigana = { listOf(kanjiToken) },
        )

        assertEquals(
            listOf(
                RichTextPiece("漢字", ruby = "かんじ", bold = true),
                RichTextPiece("漢字", ruby = "かんじ"),
            ),
            pieces,
        )
    }

    // endregion

    // region closedJapaneseRuns over runs

    @Test
    fun closedRunsExcludeNonJapaneseAndOpenTail() {
        val runs = listOf(StyledRun("これはOK"), StyledRun("最後"))

        assertEquals(
            listOf("これは"),
            RichTextPlanner.closedJapaneseRuns(runs, isTail = true),
        )
        assertEquals(
            listOf("これは", "最後"),
            RichTextPlanner.closedJapaneseRuns(runs, isTail = false),
        )
    }

    @Test
    fun duplicateRunsAreReportedOnce() {
        val runs = listOf(StyledRun("漢字と漢字"))

        assertEquals(
            listOf("漢字と漢字"),
            RichTextPlanner.closedJapaneseRuns(runs, isTail = false),
        )

        val split = listOf(StyledRun("漢字"), StyledRun("!"), StyledRun("漢字"))
        assertEquals(
            listOf("漢字"),
            RichTextPlanner.closedJapaneseRuns(split, isTail = false),
        )
    }

    // endregion

    // region closedJapaneseRuns over documents

    @Test
    fun documentRunsKeepStreamingTailOpen() {
        val blocks = MarkdownParser.parse("**強調**する")

        assertEquals(
            listOf("強調"),
            RichTextPlanner.closedDocumentRuns(blocks, isComplete = false),
        )
        assertEquals(
            listOf("強調", "する"),
            RichTextPlanner.closedDocumentRuns(blocks, isComplete = true),
        )
    }

    @Test
    fun onlyTheLastDocumentUnitCanStayOpen() {
        val blocks = MarkdownParser.parse("# 見出し\n\n本文です")

        assertEquals(
            listOf("見出し"),
            RichTextPlanner.closedDocumentRuns(blocks, isComplete = false),
        )
    }

    @Test
    fun tableCellsContributeClosedRuns() {
        val blocks = MarkdownParser.parse("| 単語 | 意味 |\n|---|---|\n| 犬 | dog |")

        assertEquals(
            listOf("単語", "意味", "犬"),
            RichTextPlanner.closedDocumentRuns(blocks, isComplete = true),
        )
    }

    @Test
    fun tableCellsAreClosedEvenWhileStreaming() {
        val blocks = MarkdownParser.parse("| 単語 | 意味 |\n|---|---|\n| 犬 | いぬ |")

        assertEquals(
            listOf("単語", "意味", "犬", "いぬ"),
            RichTextPlanner.closedDocumentRuns(blocks, isComplete = false),
        )
    }

    @Test
    fun blockquoteRunsAreClosedWhenDocumentIsComplete() {
        val blocks = MarkdownParser.parse("> 漢字です")

        assertEquals(
            listOf("漢字です"),
            RichTextPlanner.closedDocumentRuns(blocks, isComplete = true),
        )
    }

    @Test
    fun trailingBlockquoteKeepsStreamingTailOpen() {
        val blocks = MarkdownParser.parse("本文\n\n> 引用中")

        assertEquals(
            listOf("本文"),
            RichTextPlanner.closedDocumentRuns(blocks, isComplete = false),
        )
        assertEquals(
            listOf("本文", "引用中"),
            RichTextPlanner.closedDocumentRuns(blocks, isComplete = true),
        )
    }

    @Test
    fun trailingHorizontalRuleContributesNothingAndClosesPreviousRuns() {
        val blocks = MarkdownParser.parse("漢字\n\n---")

        assertEquals(
            listOf("漢字"),
            RichTextPlanner.closedDocumentRuns(blocks, isComplete = false),
        )
    }

    @Test
    fun fencedCodeBlocksAreIgnored() {
        val blocks = MarkdownParser.parse("```\n漢字\n```")

        assertEquals(
            emptyList<String>(),
            RichTextPlanner.closedDocumentRuns(blocks, isComplete = true),
        )
    }

    // endregion
}
