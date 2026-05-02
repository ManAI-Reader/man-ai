package com.highliuk.manai.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "translation_result",
    primaryKeys = ["mangaId", "pageIndex", "regionIndex", "provider"],
    foreignKeys = [
        ForeignKey(
            entity = MangaEntity::class,
            parentColumns = ["id"],
            childColumns = ["mangaId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["mangaId", "pageIndex"])]
)
data class TranslationResultEntity(
    val mangaId: Long,
    val pageIndex: Int,
    val regionIndex: Int,
    val provider: String,
    val sourceText: String,
    val translatedText: String,
    val targetLang: String,
    val timestamp: Long,
)
