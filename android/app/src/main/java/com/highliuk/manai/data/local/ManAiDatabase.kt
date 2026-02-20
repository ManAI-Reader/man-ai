package com.highliuk.manai.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.highliuk.manai.data.local.dao.MangaDao
import com.highliuk.manai.data.local.entity.MangaEntity

@Database(entities = [MangaEntity::class], version = 3, exportSchema = true)
abstract class ManAiDatabase : RoomDatabase() {
    abstract fun mangaDao(): MangaDao

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
    }
}
