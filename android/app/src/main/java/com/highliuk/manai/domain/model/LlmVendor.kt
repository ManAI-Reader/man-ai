package com.highliuk.manai.domain.model

/**
 * Fixed LLM provider a prompt template (and the conversations launched from
 * it) sends requests to. Each vendor carries the model preselected when the
 * user picks it.
 *
 * DeepSeek defaults to `deepseek-v4-flash`: the former `deepseek-chat`
 * default reached end of life on 2026-07-24.
 */
enum class LlmVendor(val defaultModel: String) {
    GROQ("openai/gpt-oss-120b"),
    DEEPSEEK("deepseek-v4-flash");

    /**
     * Reasoning levels this vendor's API actually accepts, verified
     * empirically against the live endpoints (2026-08):
     *
     * - Groq gpt-oss models accept `reasoning_effort` of low/medium/high
     *   only; `"none"`/`"default"`/`"max"` are rejected with HTTP 400, so
     *   reasoning cannot be disabled ([ReasoningLevel.OFF] is unsupported)
     *   and there is no [ReasoningLevel.MAX].
     * - DeepSeek v4 models think by default; `"none"` disables thinking and
     *   low/high/max are documented values. `"medium"` happens to be
     *   accepted but is undocumented, so it is not exposed.
     */
    val supportedReasoningLevels: List<ReasoningLevel>
        get() = when (this) {
            GROQ -> listOf(
                ReasoningLevel.DEFAULT,
                ReasoningLevel.LOW,
                ReasoningLevel.MEDIUM,
                ReasoningLevel.HIGH,
            )
            DEEPSEEK -> listOf(
                ReasoningLevel.DEFAULT,
                ReasoningLevel.OFF,
                ReasoningLevel.LOW,
                ReasoningLevel.HIGH,
                ReasoningLevel.MAX,
            )
        }

    companion object {
        /**
         * Parses a stored enum name, falling back to [GROQ] when the value is
         * null, unknown or otherwise invalid.
         */
        fun valueOfOrDefault(name: String?): LlmVendor =
            entries.firstOrNull { it.name == name } ?: GROQ
    }
}
