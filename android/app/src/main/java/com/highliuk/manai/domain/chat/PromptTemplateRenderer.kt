package com.highliuk.manai.domain.chat

import com.highliuk.manai.domain.model.PageRegion

/**
 * Context data used to resolve the placeholders of a prompt template.
 *
 * @property text full OCR text of the source balloon.
 * @property selection user-selected substring, falls back to [text] when null.
 * @property translation current translation, rendered as empty when null.
 * @property title title of the manga the balloon belongs to.
 * @property sourceRegionIndex region index of the source balloon, excluded
 * from the `{balloons}` list.
 * @property pageRegions all OCR regions of the source page.
 * @property previousPageRegions OCR regions of the nearest previous page that
 * has any OCR'd balloon, empty when no such page exists.
 * @property noPageBalloonsFallback localized text used when `{balloons}`
 * resolves to no entries.
 * @property noPreviousBalloonsFallback localized text used when
 * `{prev_balloons}` resolves to no entries.
 */
data class PromptContext(
    val text: String,
    val selection: String? = null,
    val translation: String? = null,
    val title: String = "",
    val sourceRegionIndex: Int = -1,
    val pageRegions: List<PageRegion> = emptyList(),
    val previousPageRegions: List<PageRegion> = emptyList(),
    val noPageBalloonsFallback: String = "",
    val noPreviousBalloonsFallback: String = "",
)

/**
 * Pure rendering rules for prompt templates.
 *
 * Supported placeholders: `{text}` (full balloon text), `{selection}`
 * (user-selected substring, falls back to the full text), `{translation}`
 * (current translation, empty when absent), `{title}` (manga title),
 * `{balloons}` (bulleted list of the other balloons on the same page) and
 * `{prev_balloons}` (bulleted list of the balloons on the nearest previous
 * page that has any).
 */
object PromptTemplateRenderer {
    private const val TEXT = "{text}"
    private const val SELECTION = "{selection}"
    private const val TRANSLATION = "{translation}"
    private const val TITLE = "{title}"
    private const val BALLOONS = "{balloons}"
    private const val PREV_BALLOONS = "{prev_balloons}"

    fun usesTranslation(template: String): Boolean = template.contains(TRANSLATION)

    fun usesSelection(template: String): Boolean = template.contains(SELECTION)

    fun usesTitle(template: String): Boolean = template.contains(TITLE)

    fun usesBalloons(template: String): Boolean = template.contains(BALLOONS)

    fun usesPreviousBalloons(template: String): Boolean = template.contains(PREV_BALLOONS)

    fun isAvailable(template: String, hasTranslation: Boolean): Boolean =
        hasTranslation || !usesTranslation(template)

    fun render(template: String, context: PromptContext): String =
        template
            .replace(TEXT, context.text)
            .replace(SELECTION, context.selection ?: context.text)
            .replace(TRANSLATION, context.translation.orEmpty())
            .replace(TITLE, context.title)
            .replace(
                BALLOONS,
                bulletList(
                    context.pageRegions.filter { it.regionIndex != context.sourceRegionIndex },
                    context.noPageBalloonsFallback,
                ),
            )
            .replace(
                PREV_BALLOONS,
                bulletList(context.previousPageRegions, context.noPreviousBalloonsFallback),
            )

    private fun bulletList(regions: List<PageRegion>, fallback: String): String =
        regions
            .filter { !it.ocrText.isNullOrBlank() }
            .sortedBy { it.regionIndex }
            .joinToString("\n") { "- ${it.ocrText}" }
            .ifEmpty { fallback }
}
