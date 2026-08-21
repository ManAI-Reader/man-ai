package com.highliuk.manai.domain.model

/**
 * Reasoning effort requested from the LLM for a prompt template and the
 * conversations launched from it.
 *
 * [DEFAULT] means "do not send the reasoning parameter at all" so the model
 * uses its own default. [OFF] maps to the provider value `"none"`; the other
 * levels map to their lowercase names.
 */
enum class ReasoningLevel {
    DEFAULT,
    OFF,
    LOW,
    MEDIUM,
    HIGH;

    companion object {
        /**
         * Parses a stored enum name, falling back to [DEFAULT] when the value
         * is null, unknown or otherwise invalid.
         */
        fun valueOfOrDefault(name: String?): ReasoningLevel =
            entries.firstOrNull { it.name == name } ?: DEFAULT
    }
}
