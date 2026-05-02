package com.highliuk.manai.di

import android.content.ContentResolver
import android.content.Context
import androidx.room.Room
import com.highliuk.manai.data.local.ManAiDatabase
import com.highliuk.manai.data.local.dao.MangaDao
import com.highliuk.manai.data.local.dao.PageOcrResultDao
import com.highliuk.manai.data.local.dao.TranslationResultDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import javax.inject.Singleton

@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [DatabaseModule::class]
)
object TestDatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): ManAiDatabase =
        Room.inMemoryDatabaseBuilder(context, ManAiDatabase::class.java)
            .allowMainThreadQueries()
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
    fun provideContentResolver(@ApplicationContext context: Context): ContentResolver =
        context.contentResolver
}
