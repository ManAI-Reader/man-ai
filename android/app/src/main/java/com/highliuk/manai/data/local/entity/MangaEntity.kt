package com.highliuk.manai.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.highliuk.manai.domain.model.Manga

@Entity(
    tableName = "manga",
    indices = [
        Index(value = ["contentHash"], unique = true),
        Index(value = ["uri"])
    ]
)
data class MangaEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val uri: String,
    val title: String,
    val pageCount: Int,
    @ColumnInfo(defaultValue = "0")
    val lastReadPage: Int = 0,
    @ColumnInfo(defaultValue = "")
    val contentHash: String = ""
) {
    fun toManga(): Manga = Manga(
        id = id,
        uri = uri,
        title = title,
        pageCount = pageCount,
        lastReadPage = lastReadPage,
        contentHash = contentHash
    )

    companion object {
        fun fromManga(manga: Manga): MangaEntity = MangaEntity(
            id = manga.id,
            uri = manga.uri,
            title = manga.title,
            pageCount = manga.pageCount,
            lastReadPage = manga.lastReadPage,
            contentHash = manga.contentHash
        )
    }
}
