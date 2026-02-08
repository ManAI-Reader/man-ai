package com.highliuk.manai.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "manga")
data class MangaEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val filePath: String,
    val pageCount: Int,
    val lastReadPage: Int = 0,
    val addedAt: Long = System.currentTimeMillis(),
)
