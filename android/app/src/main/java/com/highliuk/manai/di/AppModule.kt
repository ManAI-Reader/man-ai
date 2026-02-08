package com.highliuk.manai.di

import android.content.ContentResolver
import android.content.Context
import com.highliuk.manai.data.pdf.PdfDocumentHandlerImpl
import com.highliuk.manai.data.repository.MangaRepositoryImpl
import com.highliuk.manai.domain.repository.MangaRepository
import com.highliuk.manai.domain.repository.PdfDocumentHandler
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {
    @Binds
    @Singleton
    abstract fun bindMangaRepository(impl: MangaRepositoryImpl): MangaRepository

    @Binds
    @Singleton
    abstract fun bindPdfDocumentHandler(impl: PdfDocumentHandlerImpl): PdfDocumentHandler

    companion object {
        @Provides
        @Singleton
        fun provideContentResolver(@ApplicationContext context: Context): ContentResolver =
            context.contentResolver
    }
}
