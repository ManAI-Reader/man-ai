package com.highliuk.manai.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "conversation",
    foreignKeys = [
        ForeignKey(
            entity = MangaEntity::class,
            parentColumns = ["id"],
            childColumns = ["mangaId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index(value = ["mangaId"])]
)
data class ConversationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String,
    val mangaId: Long?,
    val pageIndex: Int?,
    val regionIndex: Int?,
    val createdAt: Long,
    val updatedAt: Long,
)
