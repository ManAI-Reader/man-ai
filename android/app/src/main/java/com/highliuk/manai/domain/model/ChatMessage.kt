package com.highliuk.manai.domain.model

enum class ChatRole { USER, ASSISTANT }

data class ChatMessage(
    val id: Long = 0L,
    val conversationId: Long,
    val role: ChatRole,
    val content: String,
    val timestamp: Long,
)
