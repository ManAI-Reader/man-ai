package com.highliuk.manai.ui.reader

import com.highliuk.manai.domain.model.PromptTemplate
import org.junit.Assert.assertEquals
import org.junit.Test

class OcrBottomSheetPromptsUnitTest {

    private val explain = PromptTemplate(id = 1L, name = "Explain", template = "Explain {text}")
    private val compare = PromptTemplate(
        id = 2L,
        name = "Compare",
        template = "Compare {text} with {translation}",
    )
    private val grammar = PromptTemplate(id = 3L, name = "Grammar", template = "Grammar of {selection}")

    @Test
    fun keepsAllTemplatesWhenTranslationIsAvailable() {
        val result = visiblePromptTemplates(
            templates = listOf(explain, compare, grammar),
            hasTranslation = true,
        )
        assertEquals(listOf(explain, compare, grammar), result)
    }

    @Test
    fun hidesTranslationTemplatesWhenTranslationIsMissing() {
        val result = visiblePromptTemplates(
            templates = listOf(explain, compare, grammar),
            hasTranslation = false,
        )
        assertEquals(listOf(explain, grammar), result)
    }

    @Test
    fun returnsEmptyListForEmptyInput() {
        val result = visiblePromptTemplates(templates = emptyList(), hasTranslation = true)
        assertEquals(emptyList<PromptTemplate>(), result)
    }
}
