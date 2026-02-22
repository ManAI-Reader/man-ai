package com.highliuk.manai.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "page_ocr_result",
    primaryKeys = ["mangaId", "pageIndex", "regionIndex"],
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
data class PageOcrResultEntity(
    val mangaId: Long,
    val pageIndex: Int,
    val regionIndex: Int,
    val normX1: Float,
    val normY1: Float,
    val normX2: Float,
    val normY2: Float,
    val confidence: Float,
    val ocrText: String?,
)
