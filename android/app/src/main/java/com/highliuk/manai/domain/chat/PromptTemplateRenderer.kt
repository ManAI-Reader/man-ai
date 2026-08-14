package com.highliuk.manai.domain.chat

/**
 * Pure rendering rules for prompt templates.
 *
 * Supported placeholders: `{text}` (full balloon text), `{selection}`
 * (user-selected substring, falls back to the full text), `{translation}`
 * (current translation, empty when absent).
 */
object PromptTemplateRenderer {
    private const val TEXT = "{text}"
    private const val SELECTION = "{selection}"
    private const val TRANSLATION = "{translation}"

    fun usesTranslation(template: String): Boolean = template.contains(TRANSLATION)

    fun usesSelection(template: String): Boolean = template.contains(SELECTION)

    fun isAvailable(template: String, hasTranslation: Boolean): Boolean =
        hasTranslation || !usesTranslation(template)

    fun render(template: String, text: String, selection: String?, translation: String?): String =
        template
            .replace(TEXT, text)
            .replace(SELECTION, selection ?: text)
            .replace(TRANSLATION, translation.orEmpty())
}
