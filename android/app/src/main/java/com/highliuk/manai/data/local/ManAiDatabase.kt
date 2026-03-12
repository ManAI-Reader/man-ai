package com.highliuk.manai.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.highliuk.manai.data.local.dao.MangaDao
import com.highliuk.manai.data.local.dao.PageOcrResultDao
import com.highliuk.manai.data.local.entity.MangaEntity
import com.highliuk.manai.data.local.entity.PageOcrResultEntity

@Database(
    entities = [MangaEntity::class, PageOcrResultEntity::class],
    version = 4,
    exportSchema = true,
)
abstract class ManAiDatabase : RoomDatabase() {
    abstract fun mangaDao(): MangaDao
    abstract fun pageOcrResultDao(): PageOcrResultDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE manga ADD COLUMN lastReadPage INTEGER NOT NULL DEFAULT 0"
                )
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE manga ADD COLUMN contentHash TEXT NOT NULL DEFAULT ''"
                )
                db.execSQL("DROP INDEX IF EXISTS index_manga_uri")
                db.execSQL(
                    "CREATE UNIQUE INDEX index_manga_contentHash ON manga(contentHash)"
                )
                db.execSQL("CREATE INDEX index_manga_uri ON manga(uri)")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS page_ocr_result (" +
                        "mangaId INTEGER NOT NULL, " +
                        "pageIndex INTEGER NOT NULL, " +
                        "regionIndex INTEGER NOT NULL, " +
                        "normX1 REAL NOT NULL, " +
                        "normY1 REAL NOT NULL, " +
                        "normX2 REAL NOT NULL, " +
                        "normY2 REAL NOT NULL, " +
                        "confidence REAL NOT NULL, " +
                        "ocrText TEXT, " +
                        "PRIMARY KEY(mangaId, pageIndex, regionIndex), " +
                        "FOREIGN KEY(mangaId) REFERENCES manga(id) ON DELETE CASCADE)"
                )
                db.execSQL(
                    "CREATE INDEX index_page_ocr_result_mangaId_pageIndex " +
                        "ON page_ocr_result(mangaId, pageIndex)"
                )
            }
        }

        val MIGRATION_3_2 = object : Migration(3, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP INDEX IF EXISTS index_manga_contentHash")
                db.execSQL("DROP INDEX IF EXISTS index_manga_uri")
                db.execSQL(
                    """CREATE TABLE manga_backup (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        uri TEXT NOT NULL,
                        title TEXT NOT NULL,
                        pageCount INTEGER NOT NULL,
                        lastReadPage INTEGER NOT NULL DEFAULT 0
                    )""".trimIndent()
                )
                db.execSQL(
                    "INSERT INTO manga_backup (id, uri, title, pageCount, lastReadPage) " +
                        "SELECT id, uri, title, pageCount, lastReadPage FROM manga"
                )
                db.execSQL("DROP TABLE manga")
                db.execSQL("ALTER TABLE manga_backup RENAME TO manga")
                db.execSQL("CREATE UNIQUE INDEX index_manga_uri ON manga(uri)")
            }
        }

        val MIGRATION_2_1 = object : Migration(2, 1) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP INDEX IF EXISTS index_manga_uri")
                db.execSQL(
                    """CREATE TABLE manga_backup (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        uri TEXT NOT NULL,
                        title TEXT NOT NULL,
                        pageCount INTEGER NOT NULL
                    )""".trimIndent()
                )
                db.execSQL(
                    "INSERT INTO manga_backup (id, uri, title, pageCount) " +
                        "SELECT id, uri, title, pageCount FROM manga"
                )
                db.execSQL("DROP TABLE manga")
                db.execSQL("ALTER TABLE manga_backup RENAME TO manga")
                db.execSQL("CREATE UNIQUE INDEX index_manga_uri ON manga(uri)")
            }
        }
    }
}
