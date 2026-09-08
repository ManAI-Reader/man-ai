package com.highliuk.manai.di

import android.content.ContentResolver
import android.content.Context
import androidx.room.Room
import com.highliuk.manai.data.local.ManAiDatabase
import com.highliuk.manai.data.local.dao.ChatMessageDao
import com.highliuk.manai.data.local.dao.ConversationDao
import com.highliuk.manai.data.local.dao.MangaDao
import com.highliuk.manai.data.local.dao.MemoryEntryDao
import com.highliuk.manai.data.local.dao.PageOcrResultDao
import com.highliuk.manai.data.local.dao.PromptTemplateDao
import com.highliuk.manai.data.local.dao.TranslationResultDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): ManAiDatabase =
        Room.databaseBuilder(context, ManAiDatabase::class.java, "manai.db")
            .addMigrations(
                ManAiDatabase.MIGRATION_1_2,
                ManAiDatabase.MIGRATION_2_3,
                ManAiDatabase.MIGRATION_3_4,
                ManAiDatabase.MIGRATION_4_3,
                ManAiDatabase.MIGRATION_4_5,
                ManAiDatabase.MIGRATION_5_4,
                ManAiDatabase.MIGRATION_5_6,
                ManAiDatabase.MIGRATION_6_5,
                ManAiDatabase.MIGRATION_6_7,
                ManAiDatabase.MIGRATION_7_6,
                ManAiDatabase.MIGRATION_7_8,
                ManAiDatabase.MIGRATION_8_7,
                ManAiDatabase.MIGRATION_3_2,
                ManAiDatabase.MIGRATION_2_1
            )
            .fallbackToDestructiveMigrationOnDowngrade()
            .build()

    @Provides
    fun provideMangaDao(database: ManAiDatabase): MangaDao = database.mangaDao()

    @Provides
    fun providePageOcrResultDao(database: ManAiDatabase): PageOcrResultDao =
        database.pageOcrResultDao()

    @Provides
    fun provideTranslationResultDao(database: ManAiDatabase): TranslationResultDao =
        database.translationResultDao()

    @Provides
    fun provideConversationDao(database: ManAiDatabase): ConversationDao =
        database.conversationDao()

    @Provides
    fun provideChatMessageDao(database: ManAiDatabase): ChatMessageDao =
        database.chatMessageDao()

    @Provides
    fun providePromptTemplateDao(database: ManAiDatabase): PromptTemplateDao =
        database.promptTemplateDao()

    @Provides
    fun provideMemoryEntryDao(database: ManAiDatabase): MemoryEntryDao =
        database.memoryEntryDao()

    @Provides
    fun provideContentResolver(@ApplicationContext context: Context): ContentResolver =
        context.contentResolver
}
