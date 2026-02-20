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

    @Test
    fun `toManga maps lastReadPage`() {
        val entity = MangaEntity(
            id = 1L,
            uri = "content://test.pdf",
            title = "Test Manga",
            pageCount = 42,
            lastReadPage = 15
        )

        val manga = entity.toManga()

        assertEquals(15, manga.lastReadPage)
    }

    @Test
    fun `fromManga maps lastReadPage`() {
        val manga = Manga(
            id = 1L,
            uri = "content://test.pdf",
            title = "Test Manga",
            pageCount = 42,
            lastReadPage = 7
        )

        val entity = MangaEntity.fromManga(manga)

        assertEquals(7, entity.lastReadPage)
    }

    @Test
    fun `equals returns true for same data`() {
        val entity1 = MangaEntity(id = 1L, uri = "uri", title = "T", pageCount = 10)
        val entity2 = MangaEntity(id = 1L, uri = "uri", title = "T", pageCount = 10)

        assertEquals(entity1, entity2)
    }

    @Test
    fun `equals returns false for different data`() {
        val entity1 = MangaEntity(id = 1L, uri = "uri", title = "T", pageCount = 10)
        val entity2 = MangaEntity(id = 2L, uri = "uri", title = "T", pageCount = 10)

        org.junit.Assert.assertNotEquals(entity1, entity2)
    }

    @Test
    fun `hashCode is consistent for equal entities`() {
        val entity1 = MangaEntity(id = 1L, uri = "uri", title = "T", pageCount = 10)
        val entity2 = MangaEntity(id = 1L, uri = "uri", title = "T", pageCount = 10)

        assertEquals(entity1.hashCode(), entity2.hashCode())
    }

    @Test
    fun `toString contains field values`() {
        val entity = MangaEntity(id = 1L, uri = "uri", title = "T", pageCount = 10)

        val str = entity.toString()
        org.junit.Assert.assertTrue(str.contains("uri"))
        org.junit.Assert.assertTrue(str.contains("T"))
    }

    @Test
    fun `copy creates modified entity`() {
        val entity = MangaEntity(id = 1L, uri = "uri", title = "T", pageCount = 10)

        val copied = entity.copy(title = "New")

        assertEquals("New", copied.title)
        assertEquals(1L, copied.id)
    }

    @Test
    fun `id defaults to zero`() {
        val entity = MangaEntity(uri = "content://test.pdf", title = "Test", pageCount = 5)

        assertEquals(0L, entity.id)
    }

    @Test
    fun `lastReadPage defaults to zero`() {
        val entity = MangaEntity(
            id = 1L,
            uri = "content://test.pdf",
            title = "Test Manga",
            pageCount = 42
        )

        assertEquals(0, entity.toManga().lastReadPage)
    }

    @Test
    fun `toManga maps contentHash`() {
        val entity = MangaEntity(
            id = 1L,
            uri = "content://test.pdf",
            title = "Test Manga",
            pageCount = 42,
            contentHash = "abc123hash"
        )

        val manga = entity.toManga()

        assertEquals("abc123hash", manga.contentHash)
    }

    @Test
    fun `fromManga maps contentHash`() {
        val manga = Manga(
            id = 1L,
            uri = "content://test.pdf",
            title = "Test Manga",
            pageCount = 42,
            contentHash = "def456hash"
        )

        val entity = MangaEntity.fromManga(manga)

        assertEquals("def456hash", entity.contentHash)
    }
}
