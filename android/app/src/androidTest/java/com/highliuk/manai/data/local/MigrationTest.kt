package com.highliuk.manai.data.local

import androidx.room.testing.MigrationTestHelper
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
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
