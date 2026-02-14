package com.highliuk.manai.domain.model

data class Manga(
    val id: Long = 0L,
    val uri: String,
    val title: String,
    val pageCount: Int
)
