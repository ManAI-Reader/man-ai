package com.highliuk.manai.domain.model

data class Manga(
    val id: Long = 0,
    val title: String,
    val filePath: String,
    val pageCount: Int,
    val lastReadPage: Int = 0,
    val addedAt: Long = System.currentTimeMillis(),
)
