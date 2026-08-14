package com.highliuk.manai.domain.model

data class Conversation(
    val id: Long = 0L,
    val title: String,
    val mangaId: Long? = null,
    val pageIndex: Int? = null,
    val regionIndex: Int? = null,
    val createdAt: Long,
    val updatedAt: Long,
)
