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
    fun migration5to6_createsConversationTables() {
        helper.createDatabase("test-db-5to6", 5).apply {
            execSQL(
                "INSERT INTO manga (uri, title, pageCount, lastReadPage, contentHash) " +
                    "VALUES ('content://test', 'Test', 10, 0, 'hash5to6')"
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(
            "test-db-5to6", 6, true, ManAiDatabase.MIGRATION_5_6
        )

        db.execSQL("PRAGMA foreign_keys = ON")
        db.execSQL(
            "INSERT INTO conversation (title, mangaId, pageIndex, regionIndex, createdAt, updatedAt) " +
                "VALUES ('Chat', 1, 3, 0, 1000, 2000)"
        )
        db.execSQL(
            "INSERT INTO chat_message (conversationId, role, content, timestamp) " +
                "VALUES (1, 'user', 'Hello', 1500)"
        )

        val conversationCursor = db.query(
            "SELECT title, mangaId, pageIndex, createdAt, updatedAt FROM conversation"
        )
        assertTrue(conversationCursor.moveToFirst())
        assertEquals("Chat", conversationCursor.getString(0))
        assertEquals(1, conversationCursor.getInt(1))
        assertEquals(3, conversationCursor.getInt(2))
        assertEquals(1000, conversationCursor.getLong(3))
        assertEquals(2000, conversationCursor.getLong(4))
        conversationCursor.close()

        val messageCursor = db.query(
            "SELECT conversationId, role, content, timestamp FROM chat_message"
        )
        assertTrue(messageCursor.moveToFirst())
        assertEquals(1, messageCursor.getInt(0))
        assertEquals("user", messageCursor.getString(1))
        assertEquals("Hello", messageCursor.getString(2))
        assertEquals(1500, messageCursor.getLong(3))
        messageCursor.close()

        db.execSQL("DELETE FROM manga WHERE id = 1")

        val setNullCursor = db.query("SELECT mangaId FROM conversation WHERE id = 1")
        assertTrue(setNullCursor.moveToFirst())
        assertTrue(setNullCursor.isNull(0))
        setNullCursor.close()

        db.execSQL("DELETE FROM conversation WHERE id = 1")

        val cascadeCursor = db.query("SELECT COUNT(*) FROM chat_message")
        cascadeCursor.moveToFirst()
        assertEquals(0, cascadeCursor.getInt(0))
        cascadeCursor.close()
        db.close()
    }

    @Test
    fun migration6to5_dropsAgentTables() {
        helper.createDatabase("test-db-6to5", 6).apply {
            execSQL(
                "INSERT INTO conversation (title, mangaId, pageIndex, regionIndex, createdAt, updatedAt) " +
                    "VALUES ('Chat', NULL, NULL, NULL, 1000, 2000)"
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(
            "test-db-6to5", 5, true, ManAiDatabase.MIGRATION_6_5
        )

        val cursor = db.query(
            "SELECT name FROM sqlite_master WHERE type = 'table' AND name IN " +
                "('conversation', 'chat_message', 'prompt_template', 'memory_entry')"
        )
        assertEquals(0, cursor.count)
        cursor.close()
        db.close()
    }

    @Test
    fun migration6to7_addsReasoningLevelColumnsWithDefault() {
        helper.createDatabase("test-db-6to7", 6).apply {
            execSQL(
                "INSERT INTO prompt_template (name, template, sortOrder) " +
                    "VALUES ('Explain', 'Explain {text}', 2)"
            )
            execSQL(
                "INSERT INTO conversation (title, mangaId, pageIndex, regionIndex, createdAt, updatedAt) " +
                    "VALUES ('Chat', NULL, 3, 0, 1000, 2000)"
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(
            "test-db-6to7", 7, true, ManAiDatabase.MIGRATION_6_7
        )

        val templateCursor = db.query(
            "SELECT name, template, sortOrder, reasoningLevel FROM prompt_template"
        )
        assertTrue(templateCursor.moveToFirst())
        assertEquals("Explain", templateCursor.getString(0))
        assertEquals("Explain {text}", templateCursor.getString(1))
        assertEquals(2, templateCursor.getInt(2))
        assertEquals("DEFAULT", templateCursor.getString(3))
        templateCursor.close()

        val conversationCursor = db.query(
            "SELECT title, pageIndex, createdAt, updatedAt, reasoningLevel FROM conversation"
        )
        assertTrue(conversationCursor.moveToFirst())
        assertEquals("Chat", conversationCursor.getString(0))
        assertEquals(3, conversationCursor.getInt(1))
        assertEquals(1000, conversationCursor.getLong(2))
        assertEquals(2000, conversationCursor.getLong(3))
        assertEquals("DEFAULT", conversationCursor.getString(4))
        conversationCursor.close()
        db.close()
    }

    @Test
    fun migration7to6_removesReasoningLevelAndPreservesData() {
        helper.createDatabase("test-db-7to6", 7).apply {
            execSQL(
                "INSERT INTO prompt_template (name, template, sortOrder, reasoningLevel) " +
                    "VALUES ('Explain', 'Explain {text}', 2, 'HIGH')"
            )
            execSQL(
                "INSERT INTO conversation " +
                    "(title, mangaId, pageIndex, regionIndex, createdAt, updatedAt, reasoningLevel) " +
                    "VALUES ('Chat', NULL, 3, 0, 1000, 2000, 'LOW')"
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(
            "test-db-7to6", 6, true, ManAiDatabase.MIGRATION_7_6
        )

        val templateCursor = db.query("SELECT name, template, sortOrder FROM prompt_template")
        assertTrue(templateCursor.moveToFirst())
        assertEquals("Explain", templateCursor.getString(0))
        assertEquals("Explain {text}", templateCursor.getString(1))
        assertEquals(2, templateCursor.getInt(2))
        assertEquals(-1, templateCursor.getColumnIndex("reasoningLevel"))
        templateCursor.close()

        val conversationCursor = db.query(
            "SELECT title, pageIndex, createdAt, updatedAt FROM conversation"
        )
        assertTrue(conversationCursor.moveToFirst())
        assertEquals("Chat", conversationCursor.getString(0))
        assertEquals(3, conversationCursor.getInt(1))
        assertEquals(1000, conversationCursor.getLong(2))
        assertEquals(2000, conversationCursor.getLong(3))
        assertEquals(-1, conversationCursor.getColumnIndex("reasoningLevel"))
        conversationCursor.close()
        db.close()
    }

    @Test
    fun migration6to7to6_roundTripPreservesRows() {
        helper.createDatabase("test-db-6to7to6", 6).apply {
            execSQL(
                "INSERT INTO prompt_template (name, template, sortOrder) " +
                    "VALUES ('Explain', 'Explain {text}', 0)"
            )
            close()
        }

        helper.runMigrationsAndValidate(
            "test-db-6to7to6", 7, true, ManAiDatabase.MIGRATION_6_7
        ).close()

        val db = helper.runMigrationsAndValidate(
            "test-db-6to7to6", 6, true, ManAiDatabase.MIGRATION_7_6
        )

        val cursor = db.query("SELECT name, template FROM prompt_template")
        assertTrue(cursor.moveToFirst())
        assertEquals("Explain", cursor.getString(0))
        assertEquals("Explain {text}", cursor.getString(1))
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
