package com.highliuk.manai.data.local.entity

import com.highliuk.manai.domain.model.Manga
import org.junit.Assert.assertEquals
import org.junit.Test

class MangaEntityTest {

    @Test
    fun `toManga maps entity to domain model`() {
        val entity = MangaEntity(
            id = 1L,
            uri = "content://test.pdf",
            title = "Test Manga",
            pageCount = 42
        )

        val manga = entity.toManga()

        assertEquals(1L, manga.id)
        assertEquals("content://test.pdf", manga.uri)
        assertEquals("Test Manga", manga.title)
        assertEquals(42, manga.pageCount)
    }

    @Test
    fun `fromManga maps domain model to entity`() {
        val manga = Manga(
            id = 5L,
            uri = "content://another.pdf",
            title = "Another Manga",
            pageCount = 100
        )

        val entity = MangaEntity.fromManga(manga)

        assertEquals(5L, entity.id)
        assertEquals("content://another.pdf", entity.uri)
        assertEquals("Another Manga", entity.title)
        assertEquals(100, entity.pageCount)
    }

    @Test
    fun `fromManga with default id maps to zero`() {
        val manga = Manga(uri = "content://test", title = "Test", pageCount = 1)

        val entity = MangaEntity.fromManga(manga)

        assertEquals(0L, entity.id)
    }
}
