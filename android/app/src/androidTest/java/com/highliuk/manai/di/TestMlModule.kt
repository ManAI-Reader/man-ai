package com.highliuk.manai.di

import android.graphics.Bitmap
import com.highliuk.manai.data.ml.OnnxSessionManager
import com.highliuk.manai.domain.ml.OcrResult
import com.highliuk.manai.domain.ml.TextDetector
import com.highliuk.manai.domain.ml.TextRecognizer
import com.highliuk.manai.domain.ml.TextRegion
import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import io.mockk.mockk
import javax.inject.Singleton

@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [MlModule::class]
)
object TestMlModule {

    @Provides
    @Singleton
    fun provideTextDetector(): TextDetector = object : TextDetector {
        override suspend fun detect(bitmap: Bitmap): List<TextRegion> = emptyList()
    }

    @Provides
    @Singleton
    fun provideTextRecognizer(): TextRecognizer = object : TextRecognizer {
        override suspend fun recognize(bitmap: Bitmap, region: TextRegion): OcrResult =
            OcrResult("", region)
    }

    @Provides
    @Singleton
    fun provideOnnxSessionManager(): OnnxSessionManager = mockk(relaxed = true)
}
