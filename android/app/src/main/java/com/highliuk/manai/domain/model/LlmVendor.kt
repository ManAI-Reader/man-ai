package com.highliuk.manai.domain.model

/**
 * Fixed LLM provider a prompt template (and the conversations launched from
 * it) sends requests to. Each vendor carries the model preselected when the
 * user picks it.
 */
enum class LlmVendor(val defaultModel: String) {
    GROQ("openai/gpt-oss-120b"),
    DEEPSEEK("deepseek-chat");

    companion object {
        /**
         * Parses a stored enum name, falling back to [GROQ] when the value is
         * null, unknown or otherwise invalid.
         */
        fun valueOfOrDefault(name: String?): LlmVendor =
            entries.firstOrNull { it.name == name } ?: GROQ
    }
}
