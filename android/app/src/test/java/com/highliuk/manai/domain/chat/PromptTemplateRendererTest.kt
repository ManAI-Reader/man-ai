package com.highliuk.manai.domain.chat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PromptTemplateRendererTest {

    // --- render ---

    @Test
    fun renderReplacesTextPlaceholder() {
        val result = PromptTemplateRenderer.render(
            template = "Explain: {text}",
            text = "こんにちは",
            selection = null,
            translation = null,
        )
        assertEquals("Explain: こんにちは", result)
    }

    @Test
    fun renderReplacesAllOccurrencesOfEachPlaceholder() {
        val result = PromptTemplateRenderer.render(
            template = "{text}|{text} sel={selection}/{selection} tr={translation}{translation}",
            text = "T",
            selection = "S",
            translation = "X",
        )
        assertEquals("T|T sel=S/S tr=XX", result)
    }

    @Test
    fun renderReplacesSelectionPlaceholderWithSelection() {
        val result = PromptTemplateRenderer.render(
            template = "What does {selection} mean?",
            text = "full balloon text",
            selection = "balloon",
            translation = null,
        )
        assertEquals("What does balloon mean?", result)
    }

    @Test
    fun renderFallsBackToFullTextWhenSelectionIsNull() {
        val result = PromptTemplateRenderer.render(
            template = "What does {selection} mean?",
            text = "full balloon text",
            selection = null,
            translation = null,
        )
        assertEquals("What does full balloon text mean?", result)
    }

    @Test
    fun renderReplacesTranslationPlaceholder() {
        val result = PromptTemplateRenderer.render(
            template = "Compare {text} with {translation}",
            text = "原文",
            selection = null,
            translation = "translated",
        )
        assertEquals("Compare 原文 with translated", result)
    }

    @Test
    fun renderReplacesTranslationWithEmptyStringWhenNull() {
        val result = PromptTemplateRenderer.render(
            template = "Translation: {translation}",
            text = "原文",
            selection = null,
            translation = null,
        )
        assertEquals("Translation: ", result)
    }

    // --- usesTranslation / usesSelection ---

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

    // --- isAvailable ---

    @Test
    fun isAvailableFalseOnlyWhenTemplateUsesTranslationWithoutTranslation() {
        assertFalse(PromptTemplateRenderer.isAvailable("compare {translation}", hasTranslation = false))
        assertTrue(PromptTemplateRenderer.isAvailable("compare {translation}", hasTranslation = true))
        assertTrue(PromptTemplateRenderer.isAvailable("explain {text}", hasTranslation = false))
        assertTrue(PromptTemplateRenderer.isAvailable("explain {text}", hasTranslation = true))
    }
}
