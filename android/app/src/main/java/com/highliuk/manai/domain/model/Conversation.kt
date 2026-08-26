package com.highliuk.manai.domain.model

data class Conversation(
    val id: Long = 0L,
    val title: String,
    val mangaId: Long? = null,
    val pageIndex: Int? = null,
    val regionIndex: Int? = null,
    val createdAt: Long,
    val updatedAt: Long,
    /**
     * Reasoning level snapshot copied from the prompt template at launch time
     * so follow-up turns keep using the same level.
     */
    val reasoningLevel: ReasoningLevel = ReasoningLevel.DEFAULT,
    /**
     * Vendor and model snapshot copied from the prompt template at launch
     * time so follow-up turns keep talking to the same provider and model.
     */
    val vendor: LlmVendor = LlmVendor.GROQ,
    val model: String = LlmVendor.GROQ.defaultModel,
)
