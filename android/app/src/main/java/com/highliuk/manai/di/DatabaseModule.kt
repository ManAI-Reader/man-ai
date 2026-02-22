package com.highliuk.manai.di

import android.content.Context
import androidx.room.Room
import com.highliuk.manai.data.local.ManAiDatabase
import com.highliuk.manai.data.local.dao.MangaDao
import com.highliuk.manai.data.local.dao.PageOcrResultDao
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
            )
            .build()

    @Provides
    fun provideMangaDao(database: ManAiDatabase): MangaDao = database.mangaDao()

    @Provides
    fun providePageOcrResultDao(database: ManAiDatabase): PageOcrResultDao =
        database.pageOcrResultDao()
}
