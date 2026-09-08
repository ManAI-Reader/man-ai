package com.highliuk.manai.domain.llm

import com.highliuk.manai.domain.model.LlmVendor
import com.highliuk.manai.domain.model.ReasoningLevel

/**
 * Per-request LLM settings: which vendor to call, which model to ask for and
 * the reasoning effort to request. Snapshotted on the conversation at launch
 * time so every round of a chat uses the same configuration.
 */
data class LlmRequestConfig(
    val vendor: LlmVendor,
    val model: String,
    val reasoning: ReasoningLevel,
)
