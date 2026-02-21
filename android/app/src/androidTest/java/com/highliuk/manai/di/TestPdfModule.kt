package com.highliuk.manai.di

import com.highliuk.manai.data.pdf.PdfFileCopier
import com.highliuk.manai.data.pdf.PdfMetadataExtractor
import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import javax.inject.Singleton

@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [PdfModule::class]
)
object TestPdfModule {
    @Provides
    @Singleton
    fun providePdfMetadataExtractor(): PdfMetadataExtractor =
        object : PdfMetadataExtractor {
            override suspend fun extractPageCount(uri: String): Int = 5
        }

    @Provides
    @Singleton
    fun providePdfFileCopier(): PdfFileCopier =
        object : PdfFileCopier {
            override suspend fun copyToLocalStorage(sourceUri: String): String = sourceUri
        }
}
