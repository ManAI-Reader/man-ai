package com.highliuk.manai.domain.chat

import com.highliuk.manai.domain.model.PageRegion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PromptTemplateRendererTest {

    private fun region(index: Int, text: String?) = PageRegion(
        regionIndex = index,
        normX1 = 0f,
        normY1 = 0f,
        normX2 = 1f,
        normY2 = 1f,
        confidence = 0.9f,
        ocrText = text,
        pageIndex = 3,
    )

    private fun context(
        text: String = "text",
        selection: String? = null,
        translation: String? = null,
    ) = PromptContext(
        text = text,
        selection = selection,
        translation = translation,
        noPageBalloonsFallback = NO_PAGE_BALLOONS,
        noPreviousBalloonsFallback = NO_PREVIOUS_BALLOONS,
    )

    private fun balloonContext(
        sourceRegionIndex: Int = -1,
        pageRegions: List<PageRegion> = emptyList(),
        previousPageRegions: List<PageRegion> = emptyList(),
    ) = context().copy(
        sourceRegionIndex = sourceRegionIndex,
        pageRegions = pageRegions,
        previousPageRegions = previousPageRegions,
    )

    // --- render: original placeholders ---

    @Test
    fun renderReplacesTextPlaceholder() {
        val result = PromptTemplateRenderer.render(
            template = "Explain: {text}",
            context = context(text = "こんにちは"),
        )
        assertEquals("Explain: こんにちは", result)
    }

    @Test
    fun renderReplacesAllOccurrencesOfEachPlaceholder() {
        val result = PromptTemplateRenderer.render(
            template = "{text}|{text} sel={selection}/{selection} tr={translation}{translation}",
            context = context(text = "T", selection = "S", translation = "X"),
        )
        assertEquals("T|T sel=S/S tr=XX", result)
    }

    @Test
    fun renderReplacesSelectionPlaceholderWithSelection() {
        val result = PromptTemplateRenderer.render(
            template = "What does {selection} mean?",
            context = context(text = "full balloon text", selection = "balloon"),
        )
        assertEquals("What does balloon mean?", result)
    }

    @Test
    fun renderFallsBackToFullTextWhenSelectionIsNull() {
        val result = PromptTemplateRenderer.render(
            template = "What does {selection} mean?",
            context = context(text = "full balloon text"),
        )
        assertEquals("What does full balloon text mean?", result)
    }

    @Test
    fun renderReplacesTranslationPlaceholder() {
        val result = PromptTemplateRenderer.render(
            template = "Compare {text} with {translation}",
            context = context(text = "原文", translation = "translated"),
        )
        assertEquals("Compare 原文 with translated", result)
    }

    @Test
    fun renderReplacesTranslationWithEmptyStringWhenNull() {
        val result = PromptTemplateRenderer.render(
            template = "Translation: {translation}",
            context = context(text = "原文"),
        )
        assertEquals("Translation: ", result)
    }

    // --- render: {title} ---

    @Test
    fun renderReplacesTitlePlaceholderWithMangaTitle() {
        val result = PromptTemplateRenderer.render(
            template = "From {title}: {text}",
            context = context(text = "セリフ").copy(title = "よつばと!"),
        )
        assertEquals("From よつばと!: セリフ", result)
    }

    // --- render: {balloons} ---

    @Test
    fun renderBalloonsListsOtherBalloonsExcludingSourceInRegionIndexOrder() {
        val result = PromptTemplateRenderer.render(
            template = "Context:\n{balloons}",
            context = balloonContext(
                sourceRegionIndex = 1,
                pageRegions = listOf(
                    region(2, "三番目"),
                    region(0, "一番目"),
                    region(1, "ソース"),
                ),
            ),
        )
        assertEquals("Context:\n- 一番目\n- 三番目", result)
    }

    @Test
    fun renderBalloonsSkipsRegionsWithNullOrBlankOcrText() {
        val result = PromptTemplateRenderer.render(
            template = "{balloons}",
            context = balloonContext(
                sourceRegionIndex = 0,
                pageRegions = listOf(
                    region(0, "ソース"),
                    region(1, null),
                    region(2, "  "),
                    region(3, "有効"),
                ),
            ),
        )
        assertEquals("- 有効", result)
    }

    @Test
    fun renderBalloonsFallsBackWhenNoOtherBalloons() {
        val result = PromptTemplateRenderer.render(
            template = "{balloons}",
            context = balloonContext(
                sourceRegionIndex = 0,
                pageRegions = listOf(region(0, "ソース"), region(1, null)),
            ),
        )
        assertEquals(NO_PAGE_BALLOONS, result)
    }

    // --- render: {prev_balloons} ---

    @Test
    fun renderPrevBalloonsListsPreviousPageBalloonsInRegionIndexOrder() {
        val result = PromptTemplateRenderer.render(
            template = "{prev_balloons}",
            context = balloonContext(
                previousPageRegions = listOf(region(1, "後"), region(0, "先")),
            ),
        )
        assertEquals("- 先\n- 後", result)
    }

    @Test
    fun renderPrevBalloonsSkipsBlankAndFallsBackWhenEmpty() {
        assertEquals(
            NO_PREVIOUS_BALLOONS,
            PromptTemplateRenderer.render(
                template = "{prev_balloons}",
                context = balloonContext(previousPageRegions = listOf(region(0, null), region(1, ""))),
            ),
        )
        assertEquals(
            NO_PREVIOUS_BALLOONS,
            PromptTemplateRenderer.render(
                template = "{prev_balloons}",
                context = balloonContext(previousPageRegions = emptyList()),
            ),
        )
    }

    // --- usesTranslation / usesSelection / new tag detection ---

    @Test
    fun usesTranslationDetectsPlaceholder() {
        assertTrue(PromptTemplateRenderer.usesTranslation("check {translation} here"))
        assertFalse(PromptTemplateRenderer.usesTranslation("only {text} and {selection}"))
    }

    @Test
    fun usesSelectionDetectsPlaceholder() {
        assertTrue(PromptTemplateRenderer.usesSelection("explain {selection}"))
        assertFalse(PromptTemplateRenderer.usesSelection("only {text} and {translation}"))
    }

    @Test
    fun usesTitleDetectsPlaceholder() {
        assertTrue(PromptTemplateRenderer.usesTitle("manga {title}"))
        assertFalse(PromptTemplateRenderer.usesTitle("only {text}"))
    }

    @Test
    fun usesBalloonsDetectsPlaceholder() {
        assertTrue(PromptTemplateRenderer.usesBalloons("page {balloons}"))
        assertFalse(PromptTemplateRenderer.usesBalloons("previous {prev_balloons}"))
    }

    @Test
    fun usesPreviousBalloonsDetectsPlaceholder() {
        assertTrue(PromptTemplateRenderer.usesPreviousBalloons("prev {prev_balloons}"))
        assertFalse(PromptTemplateRenderer.usesPreviousBalloons("page {balloons}"))
    }

    // --- isAvailable ---

    @Test
    fun isAvailableFalseOnlyWhenTemplateUsesTranslationWithoutTranslation() {
        assertFalse(PromptTemplateRenderer.isAvailable("compare {translation}", hasTranslation = false))
        assertTrue(PromptTemplateRenderer.isAvailable("compare {translation}", hasTranslation = true))
        assertTrue(PromptTemplateRenderer.isAvailable("explain {text}", hasTranslation = false))
        assertTrue(PromptTemplateRenderer.isAvailable("explain {text}", hasTranslation = true))
    }

    private companion object {
        const val NO_PAGE_BALLOONS = "No other balloons on this page"
        const val NO_PREVIOUS_BALLOONS = "No balloons on the previous pages"
    }
}
