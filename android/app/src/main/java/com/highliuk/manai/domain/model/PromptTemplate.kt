package com.highliuk.manai.domain.model

data class PromptTemplate(
    val id: Long = 0L,
    val name: String,
    val template: String,
    val sortOrder: Int = 0,
    val reasoningLevel: ReasoningLevel = ReasoningLevel.DEFAULT,
    val vendor: LlmVendor = LlmVendor.GROQ,
    val model: String = LlmVendor.GROQ.defaultModel,
)
