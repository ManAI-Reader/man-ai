package com.highliuk.manai.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "memory_entry")
data class MemoryEntryEntity(
    @PrimaryKey val title: String,
    val content: String,
    val updatedAt: Long,
)
