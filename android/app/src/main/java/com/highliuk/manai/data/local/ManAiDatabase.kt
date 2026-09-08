package com.highliuk.manai.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.highliuk.manai.data.local.dao.ChatMessageDao
import com.highliuk.manai.data.local.dao.ConversationDao
import com.highliuk.manai.data.local.dao.MangaDao
import com.highliuk.manai.data.local.dao.MemoryEntryDao
import com.highliuk.manai.data.local.dao.PageOcrResultDao
import com.highliuk.manai.data.local.dao.PromptTemplateDao
import com.highliuk.manai.data.local.dao.TranslationResultDao
import com.highliuk.manai.data.local.entity.ChatMessageEntity
import com.highliuk.manai.data.local.entity.ConversationEntity
import com.highliuk.manai.data.local.entity.MangaEntity
import com.highliuk.manai.data.local.entity.MemoryEntryEntity
import com.highliuk.manai.data.local.entity.PageOcrResultEntity
import com.highliuk.manai.data.local.entity.PromptTemplateEntity
import com.highliuk.manai.data.local.entity.TranslationResultEntity

@Database(
    entities = [
        MangaEntity::class,
        PageOcrResultEntity::class,
        TranslationResultEntity::class,
        ConversationEntity::class,
        ChatMessageEntity::class,
        PromptTemplateEntity::class,
        MemoryEntryEntity::class,
    ],
    version = 8,
    exportSchema = true,
)
abstract class ManAiDatabase : RoomDatabase() {
    abstract fun mangaDao(): MangaDao
    abstract fun pageOcrResultDao(): PageOcrResultDao
    abstract fun translationResultDao(): TranslationResultDao
    abstract fun conversationDao(): ConversationDao
    abstract fun chatMessageDao(): ChatMessageDao
    abstract fun promptTemplateDao(): PromptTemplateDao
    abstract fun memoryEntryDao(): MemoryEntryDao

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

        val MIGRATION_4_3 = object : Migration(4, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP INDEX IF EXISTS index_page_ocr_result_mangaId_pageIndex")
                db.execSQL("DROP TABLE IF EXISTS page_ocr_result")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS translation_result (" +
                        "mangaId INTEGER NOT NULL, " +
                        "pageIndex INTEGER NOT NULL, " +
                        "regionIndex INTEGER NOT NULL, " +
                        "provider TEXT NOT NULL, " +
                        "sourceText TEXT NOT NULL, " +
                        "translatedText TEXT NOT NULL, " +
                        "targetLang TEXT NOT NULL, " +
                        "timestamp INTEGER NOT NULL, " +
                        "PRIMARY KEY(mangaId, pageIndex, regionIndex, provider), " +
                        "FOREIGN KEY(mangaId) REFERENCES manga(id) ON DELETE CASCADE)"
                )
                db.execSQL(
                    "CREATE INDEX index_translation_result_mangaId_pageIndex " +
                        "ON translation_result(mangaId, pageIndex)"
                )
            }
        }

        val MIGRATION_5_4 = object : Migration(5, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP INDEX IF EXISTS index_translation_result_mangaId_pageIndex")
                db.execSQL("DROP TABLE IF EXISTS translation_result")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS conversation (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "title TEXT NOT NULL, " +
                        "mangaId INTEGER, " +
                        "pageIndex INTEGER, " +
                        "regionIndex INTEGER, " +
                        "createdAt INTEGER NOT NULL, " +
                        "updatedAt INTEGER NOT NULL, " +
                        "FOREIGN KEY(mangaId) REFERENCES manga(id) ON DELETE SET NULL)"
                )
                db.execSQL("CREATE INDEX index_conversation_mangaId ON conversation(mangaId)")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS chat_message (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "conversationId INTEGER NOT NULL, " +
                        "role TEXT NOT NULL, " +
                        "content TEXT NOT NULL, " +
                        "timestamp INTEGER NOT NULL, " +
                        "FOREIGN KEY(conversationId) REFERENCES conversation(id) ON DELETE CASCADE)"
                )
                db.execSQL(
                    "CREATE INDEX index_chat_message_conversationId ON chat_message(conversationId)"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS prompt_template (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "name TEXT NOT NULL, " +
                        "template TEXT NOT NULL, " +
                        "sortOrder INTEGER NOT NULL DEFAULT 0)"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS memory_entry (" +
                        "title TEXT PRIMARY KEY NOT NULL, " +
                        "content TEXT NOT NULL, " +
                        "updatedAt INTEGER NOT NULL)"
                )
            }
        }

        val MIGRATION_6_5 = object : Migration(6, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP TABLE IF EXISTS chat_message")
                db.execSQL("DROP INDEX IF EXISTS index_conversation_mangaId")
                db.execSQL("DROP TABLE IF EXISTS conversation")
                db.execSQL("DROP TABLE IF EXISTS prompt_template")
                db.execSQL("DROP TABLE IF EXISTS memory_entry")
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE prompt_template ADD COLUMN reasoningLevel TEXT NOT NULL DEFAULT 'DEFAULT'"
                )
                db.execSQL(
                    "ALTER TABLE conversation ADD COLUMN reasoningLevel TEXT NOT NULL DEFAULT 'DEFAULT'"
                )
            }
        }

        val MIGRATION_7_6 = object : Migration(7, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """CREATE TABLE prompt_template_backup (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        template TEXT NOT NULL,
                        sortOrder INTEGER NOT NULL DEFAULT 0
                    )""".trimIndent()
                )
                db.execSQL(
                    "INSERT INTO prompt_template_backup (id, name, template, sortOrder) " +
                        "SELECT id, name, template, sortOrder FROM prompt_template"
                )
                db.execSQL("DROP TABLE prompt_template")
                db.execSQL("ALTER TABLE prompt_template_backup RENAME TO prompt_template")
                db.execSQL("DROP INDEX IF EXISTS index_conversation_mangaId")
                db.execSQL(
                    """CREATE TABLE conversation_backup (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        title TEXT NOT NULL,
                        mangaId INTEGER,
                        pageIndex INTEGER,
                        regionIndex INTEGER,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL,
                        FOREIGN KEY(mangaId) REFERENCES manga(id) ON DELETE SET NULL
                    )""".trimIndent()
                )
                db.execSQL(
                    "INSERT INTO conversation_backup " +
                        "(id, title, mangaId, pageIndex, regionIndex, createdAt, updatedAt) " +
                        "SELECT id, title, mangaId, pageIndex, regionIndex, createdAt, updatedAt " +
                        "FROM conversation"
                )
                db.execSQL("DROP TABLE conversation")
                db.execSQL("ALTER TABLE conversation_backup RENAME TO conversation")
                db.execSQL("CREATE INDEX index_conversation_mangaId ON conversation(mangaId)")
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE prompt_template ADD COLUMN vendor TEXT NOT NULL DEFAULT 'GROQ'"
                )
                db.execSQL(
                    "ALTER TABLE prompt_template ADD COLUMN model TEXT NOT NULL " +
                        "DEFAULT 'openai/gpt-oss-120b'"
                )
                db.execSQL(
                    "ALTER TABLE conversation ADD COLUMN vendor TEXT NOT NULL DEFAULT 'GROQ'"
                )
                db.execSQL(
                    "ALTER TABLE conversation ADD COLUMN model TEXT NOT NULL " +
                        "DEFAULT 'openai/gpt-oss-120b'"
                )
            }
        }

        val MIGRATION_8_7 = object : Migration(8, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """CREATE TABLE prompt_template_backup (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        template TEXT NOT NULL,
                        sortOrder INTEGER NOT NULL DEFAULT 0,
                        reasoningLevel TEXT NOT NULL DEFAULT 'DEFAULT'
                    )""".trimIndent()
                )
                db.execSQL(
                    "INSERT INTO prompt_template_backup " +
                        "(id, name, template, sortOrder, reasoningLevel) " +
                        "SELECT id, name, template, sortOrder, reasoningLevel " +
                        "FROM prompt_template"
                )
                db.execSQL("DROP TABLE prompt_template")
                db.execSQL("ALTER TABLE prompt_template_backup RENAME TO prompt_template")
                db.execSQL("DROP INDEX IF EXISTS index_conversation_mangaId")
                db.execSQL(
                    """CREATE TABLE conversation_backup (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        title TEXT NOT NULL,
                        mangaId INTEGER,
                        pageIndex INTEGER,
                        regionIndex INTEGER,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL,
                        reasoningLevel TEXT NOT NULL DEFAULT 'DEFAULT',
                        FOREIGN KEY(mangaId) REFERENCES manga(id) ON DELETE SET NULL
                    )""".trimIndent()
                )
                db.execSQL(
                    "INSERT INTO conversation_backup " +
                        "(id, title, mangaId, pageIndex, regionIndex, createdAt, updatedAt, " +
                        "reasoningLevel) " +
                        "SELECT id, title, mangaId, pageIndex, regionIndex, createdAt, " +
                        "updatedAt, reasoningLevel FROM conversation"
                )
                db.execSQL("DROP TABLE conversation")
                db.execSQL("ALTER TABLE conversation_backup RENAME TO conversation")
                db.execSQL("CREATE INDEX index_conversation_mangaId ON conversation(mangaId)")
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
