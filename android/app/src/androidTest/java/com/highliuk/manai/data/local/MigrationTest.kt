package com.highliuk.manai.data.local

import androidx.room.testing.MigrationTestHelper
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class MigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        ManAiDatabase::class.java
    )

    @Test
    fun migration2to3_addsContentHashColumn() {
        helper.createDatabase("test-db", 2).apply {
            execSQL(
                "INSERT INTO manga (uri, title, pageCount, lastReadPage) VALUES ('content://test', 'Test', 10, 0)"
            )
            close()
        }

        val db = helper.runMigrationsAndValidate("test-db", 3, true, ManAiDatabase.MIGRATION_2_3)

        val cursor = db.query("SELECT contentHash FROM manga")
        cursor.moveToFirst()
        assertEquals("", cursor.getString(0))
        cursor.close()
        db.close()
    }

    @Test
    fun migration3to4_createsPageOcrResultTable() {
        helper.createDatabase("test-db-3to4", 3).apply {
            execSQL(
                "INSERT INTO manga (uri, title, pageCount, lastReadPage, contentHash) " +
                    "VALUES ('content://test', 'Test', 10, 0, 'hash1')"
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(
            "test-db-3to4", 4, true, ManAiDatabase.MIGRATION_3_4
        )

        db.execSQL(
            "INSERT INTO page_ocr_result " +
                "(mangaId, pageIndex, regionIndex, normX1, normY1, normX2, normY2, confidence, ocrText) " +
                "VALUES (1, 0, 0, 0.1, 0.2, 0.3, 0.4, 0.95, NULL)"
        )

        val cursor = db.query("SELECT * FROM page_ocr_result WHERE mangaId = 1")
        assertTrue(cursor.moveToFirst())
        assertEquals(0, cursor.getInt(cursor.getColumnIndexOrThrow("pageIndex")))
        assertEquals(0, cursor.getInt(cursor.getColumnIndexOrThrow("regionIndex")))
        assertEquals(0.1f, cursor.getFloat(cursor.getColumnIndexOrThrow("normX1")), 0.001f)
        assertEquals(0.95f, cursor.getFloat(cursor.getColumnIndexOrThrow("confidence")), 0.001f)
        assertTrue(cursor.isNull(cursor.getColumnIndexOrThrow("ocrText")))
        cursor.close()
        db.close()
    }

    @Test
    fun migration3to4_foreignKeyCascadeDeletesOcrResults() {
        helper.createDatabase("test-db-fk", 3).apply {
            execSQL(
                "INSERT INTO manga (uri, title, pageCount, lastReadPage, contentHash) " +
                    "VALUES ('content://test', 'Test', 10, 0, 'hash2')"
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(
            "test-db-fk", 4, true, ManAiDatabase.MIGRATION_3_4
        )

        db.execSQL("PRAGMA foreign_keys = ON")
        db.execSQL(
            "INSERT INTO page_ocr_result " +
                "(mangaId, pageIndex, regionIndex, normX1, normY1, normX2, normY2, confidence, ocrText) " +
                "VALUES (1, 0, 0, 0.1, 0.2, 0.3, 0.4, 0.9, 'test')"
        )

        db.execSQL("DELETE FROM manga WHERE id = 1")

        val cursor = db.query("SELECT COUNT(*) FROM page_ocr_result")
        cursor.moveToFirst()
        assertEquals(0, cursor.getInt(0))
        cursor.close()
        db.close()
    }

    @Test
    fun migration3to2_removesContentHashAndPreservesData() {
        helper.createDatabase("test-db-down32", 3).apply {
            execSQL(
                "INSERT INTO manga (uri, title, pageCount, lastReadPage, contentHash) " +
                    "VALUES ('content://test', 'Test Manga', 10, 5, 'abc123')"
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(
            "test-db-down32", 2, true, ManAiDatabase.MIGRATION_3_2
        )

        val cursor = db.query("SELECT uri, title, pageCount, lastReadPage FROM manga")
        cursor.moveToFirst()
        assertEquals("content://test", cursor.getString(0))
        assertEquals("Test Manga", cursor.getString(1))
        assertEquals(10, cursor.getInt(2))
        assertEquals(5, cursor.getInt(3))
        assertEquals(-1, cursor.getColumnIndex("contentHash"))
        cursor.close()
        db.close()
    }

    @Test
    fun migration2to1_removesLastReadPageAndPreservesData() {
        helper.createDatabase("test-db-down21", 2).apply {
            execSQL(
                "INSERT INTO manga (uri, title, pageCount, lastReadPage) " +
                    "VALUES ('content://test', 'Test Manga', 10, 5)"
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(
            "test-db-down21", 1, true, ManAiDatabase.MIGRATION_2_1
        )

        val cursor = db.query("SELECT uri, title, pageCount FROM manga")
        cursor.moveToFirst()
        assertEquals("content://test", cursor.getString(0))
        assertEquals("Test Manga", cursor.getString(1))
        assertEquals(10, cursor.getInt(2))
        assertEquals(-1, cursor.getColumnIndex("lastReadPage"))
        cursor.close()
        db.close()
    }
}
