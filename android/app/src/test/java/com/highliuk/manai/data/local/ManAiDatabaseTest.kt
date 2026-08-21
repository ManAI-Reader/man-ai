package com.highliuk.manai.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import io.mockk.mockk
import io.mockk.verifyOrder
import org.junit.Assert.assertTrue
import org.junit.Test

class ManAiDatabaseTest {

    private val db = mockk<SupportSQLiteDatabase>(relaxed = true)

    @Test
    fun `every upgrade migration has a matching downgrade`() {
        val all: List<Migration> = ManAiDatabase::class.java.declaredFields
            .filter { Migration::class.java.isAssignableFrom(it.type) }
            .map {
                it.isAccessible = true
                it.get(null) as Migration
            }

        val ups = all.filter { it.startVersion < it.endVersion }
        val downs = all.filter { it.startVersion > it.endVersion }

        assertTrue("No migrations found via reflection", all.isNotEmpty())

        ups.forEach { up ->
            assertTrue(
                "Missing downgrade migration ${up.endVersion}→${up.startVersion}",
                downs.any { down ->
                    down.startVersion == up.endVersion && down.endVersion == up.startVersion
                }
            )
        }
    }

    @Test
    fun `migration 1 to 2 adds lastReadPage column`() {
        ManAiDatabase.MIGRATION_1_2.migrate(db)

        verifyOrder {
            db.execSQL("ALTER TABLE manga ADD COLUMN lastReadPage INTEGER NOT NULL DEFAULT 0")
        }
    }

    @Test
    fun `migration 2 to 3 adds contentHash column and swaps unique index from uri to contentHash`() {
        ManAiDatabase.MIGRATION_2_3.migrate(db)

        verifyOrder {
            db.execSQL("ALTER TABLE manga ADD COLUMN contentHash TEXT NOT NULL DEFAULT ''")
            db.execSQL("DROP INDEX IF EXISTS index_manga_uri")
            db.execSQL("CREATE UNIQUE INDEX index_manga_contentHash ON manga(contentHash)")
            db.execSQL("CREATE INDEX index_manga_uri ON manga(uri)")
        }
    }

    @Test
    fun `migration 3 to 2 removes contentHash via backup table and restores unique uri index`() {
        ManAiDatabase.MIGRATION_3_2.migrate(db)

        verifyOrder {
            db.execSQL("DROP INDEX IF EXISTS index_manga_contentHash")
            db.execSQL("DROP INDEX IF EXISTS index_manga_uri")
            db.execSQL(match { it.contains("CREATE TABLE manga_backup") && !it.contains("contentHash") })
            db.execSQL(match {
                it.contains("INSERT INTO manga_backup") &&
                    it.contains("id, uri, title, pageCount, lastReadPage")
            })
            db.execSQL("DROP TABLE manga")
            db.execSQL("ALTER TABLE manga_backup RENAME TO manga")
            db.execSQL("CREATE UNIQUE INDEX index_manga_uri ON manga(uri)")
        }
    }

    @Test
    fun `migration 5 to 6 creates agent tables`() {
        ManAiDatabase.MIGRATION_5_6.migrate(db)

        verifyOrder {
            db.execSQL(match { it.contains("CREATE TABLE IF NOT EXISTS conversation") })
            db.execSQL(match { it.contains("CREATE INDEX index_conversation_mangaId") })
            db.execSQL(match { it.contains("CREATE TABLE IF NOT EXISTS chat_message") })
            db.execSQL(match { it.contains("CREATE INDEX index_chat_message_conversationId") })
            db.execSQL(match { it.contains("CREATE TABLE IF NOT EXISTS prompt_template") })
            db.execSQL(match { it.contains("CREATE TABLE IF NOT EXISTS memory_entry") })
        }
    }

    @Test
    fun `migration 6 to 5 drops agent tables`() {
        ManAiDatabase.MIGRATION_6_5.migrate(db)

        verifyOrder {
            db.execSQL("DROP TABLE IF EXISTS chat_message")
            db.execSQL("DROP INDEX IF EXISTS index_conversation_mangaId")
            db.execSQL("DROP TABLE IF EXISTS conversation")
            db.execSQL("DROP TABLE IF EXISTS prompt_template")
            db.execSQL("DROP TABLE IF EXISTS memory_entry")
        }
    }

    @Test
    fun `migration 6 to 7 adds reasoningLevel columns`() {
        ManAiDatabase.MIGRATION_6_7.migrate(db)

        verifyOrder {
            db.execSQL(
                "ALTER TABLE prompt_template ADD COLUMN reasoningLevel TEXT NOT NULL DEFAULT 'DEFAULT'"
            )
            db.execSQL(
                "ALTER TABLE conversation ADD COLUMN reasoningLevel TEXT NOT NULL DEFAULT 'DEFAULT'"
            )
        }
    }

    @Test
    fun `migration 7 to 6 removes reasoningLevel via backup tables`() {
        ManAiDatabase.MIGRATION_7_6.migrate(db)

        verifyOrder {
            db.execSQL(match {
                it.contains("CREATE TABLE prompt_template_backup") && !it.contains("reasoningLevel")
            })
            db.execSQL(match {
                it.contains("INSERT INTO prompt_template_backup") &&
                    it.contains("id, name, template, sortOrder") &&
                    !it.contains("reasoningLevel")
            })
            db.execSQL("DROP TABLE prompt_template")
            db.execSQL("ALTER TABLE prompt_template_backup RENAME TO prompt_template")
            db.execSQL("DROP INDEX IF EXISTS index_conversation_mangaId")
            db.execSQL(match {
                it.contains("CREATE TABLE conversation_backup") && !it.contains("reasoningLevel")
            })
            db.execSQL(match {
                it.contains("INSERT INTO conversation_backup") &&
                    it.contains("id, title, mangaId, pageIndex, regionIndex, createdAt, updatedAt") &&
                    !it.contains("reasoningLevel")
            })
            db.execSQL("DROP TABLE conversation")
            db.execSQL("ALTER TABLE conversation_backup RENAME TO conversation")
            db.execSQL("CREATE INDEX index_conversation_mangaId ON conversation(mangaId)")
        }
    }

    @Test
    fun `migration 2 to 1 removes lastReadPage via backup table`() {
        ManAiDatabase.MIGRATION_2_1.migrate(db)

        verifyOrder {
            db.execSQL("DROP INDEX IF EXISTS index_manga_uri")
            db.execSQL(match {
                it.contains("CREATE TABLE manga_backup") && !it.contains("lastReadPage")
            })
            db.execSQL(match {
                it.contains("INSERT INTO manga_backup") &&
                    it.contains("id, uri, title, pageCount") &&
                    !it.contains("lastReadPage")
            })
            db.execSQL("DROP TABLE manga")
            db.execSQL("ALTER TABLE manga_backup RENAME TO manga")
            db.execSQL("CREATE UNIQUE INDEX index_manga_uri ON manga(uri)")
        }
    }
}
