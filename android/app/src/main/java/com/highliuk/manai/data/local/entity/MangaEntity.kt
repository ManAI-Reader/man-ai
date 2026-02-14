package com.highliuk.manai.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.highliuk.manai.domain.model.Manga

@Entity(
    tableName = "manga",
    indices = [Index(value = ["uri"], unique = true)]
)
data class MangaEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val uri: String,
    val title: String,
    val pageCount: Int
) {
    fun toManga(): Manga = Manga(
        id = id,
        uri = uri,
        title = title,
        pageCount = pageCount
    )

    companion object {
        fun fromManga(manga: Manga): MangaEntity = MangaEntity(
            id = manga.id,
            uri = manga.uri,
            title = manga.title,
            pageCount = manga.pageCount
        )
    }
}
