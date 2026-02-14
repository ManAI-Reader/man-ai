package com.highliuk.manai.di

import com.highliuk.manai.data.pdf.AndroidPdfMetadataExtractor
import com.highliuk.manai.data.pdf.PdfMetadataExtractor
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class PdfModule {
    @Binds
    @Singleton
    abstract fun bindPdfMetadataExtractor(impl: AndroidPdfMetadataExtractor): PdfMetadataExtractor
}
