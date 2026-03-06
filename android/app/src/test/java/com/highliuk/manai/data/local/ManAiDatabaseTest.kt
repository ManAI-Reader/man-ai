package com.highliuk.manai.data.local

import androidx.sqlite.db.SupportSQLiteDatabase
import io.mockk.mockk
import io.mockk.verifyOrder
import org.junit.Assert.assertEquals
import org.junit.Test

class ManAiDatabaseTest {

    private val db = mockk<SupportSQLiteDatabase>(relaxed = true)

    @Test
    fun downgradeMigration3to2_hasCorrectVersions() {
        val migration = ManAiDatabase.MIGRATION_3_2
        assertEquals(3, migration.startVersion)
        assertEquals(2, migration.endVersion)
    }

    @Test
    fun downgradeMigration2to1_hasCorrectVersions() {
        val migration = ManAiDatabase.MIGRATION_2_1
        assertEquals(2, migration.startVersion)
        assertEquals(1, migration.endVersion)
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
