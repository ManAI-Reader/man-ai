package com.highliuk.manai.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class MangaTest {

    @Test
    fun `manga data class holds all properties correctly`() {
        val manga = Manga(
            id = 1L,
            uri = "content://media/external/documents/test.pdf",
            title = "My Manga",
            pageCount = 42
        )

        assertEquals(1L, manga.id)
        assertEquals("content://media/external/documents/test.pdf", manga.uri)
        assertEquals("My Manga", manga.title)
        assertEquals(42, manga.pageCount)
    }

    @Test
    fun `manga default id is zero`() {
        val manga = Manga(
            uri = "content://test",
            title = "Test",
            pageCount = 10
        )

        assertEquals(0L, manga.id)
    }
}
