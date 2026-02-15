package com.highliuk.manai.data.local.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.highliuk.manai.data.local.ManAiDatabase
import com.highliuk.manai.data.local.entity.MangaEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class MangaDaoTest {

    private lateinit var database: ManAiDatabase
    private lateinit var dao: MangaDao

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            ManAiDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = database.mangaDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertAndGetAll_returnsInsertedManga() = runTest {
        val entity = MangaEntity(uri = "content://test1", title = "Manga 1", pageCount = 10)
        dao.insert(entity)

        val result = dao.getAll().first()
        assertEquals(1, result.size)
        assertEquals("Manga 1", result[0].title)
        assertEquals(10, result[0].pageCount)
    }

    @Test
    fun insertDuplicateUri_isIgnored() = runTest {
        val entity1 = MangaEntity(uri = "content://same", title = "First", pageCount = 10)
        val entity2 = MangaEntity(uri = "content://same", title = "Second", pageCount = 20)
        dao.insert(entity1)
        dao.insert(entity2)

        val result = dao.getAll().first()
        assertEquals(1, result.size)
        assertEquals("First", result[0].title)
    }

    @Test
    fun getById_returnsMatchingManga() = runTest {
        val entity = MangaEntity(uri = "content://test", title = "Test", pageCount = 10)
        dao.insert(entity)
        val all = dao.getAll().first()
        val id = all[0].id

        val result = dao.getById(id).first()
        assertEquals("Test", result?.title)
    }

    @Test
    fun getById_returnsNullForNonexistentId() = runTest {
        val result = dao.getById(999).first()
        assertNull(result)
    }

    @Test
    fun insertMultipleDifferentUris_returnsAll() = runTest {
        dao.insert(MangaEntity(uri = "content://a", title = "A", pageCount = 1))
        dao.insert(MangaEntity(uri = "content://b", title = "B", pageCount = 2))
        dao.insert(MangaEntity(uri = "content://c", title = "C", pageCount = 3))

        val result = dao.getAll().first()
        assertEquals(3, result.size)
    }
}
