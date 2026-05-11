package com.highliuk.manai.domain.model

data class FuriganaToken(
    val surface: String,
    val reading: String?,
    val parts: List<FuriganaPart>
)
