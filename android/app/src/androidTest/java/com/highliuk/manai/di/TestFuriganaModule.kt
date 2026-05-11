package com.highliuk.manai.di

import com.highliuk.manai.domain.furigana.FuriganaMatcher
import com.highliuk.manai.domain.furigana.KanjiReadingsDataSource
import com.highliuk.manai.domain.furigana.KanjiSplitter
import com.highliuk.manai.domain.ml.JapaneseTokenizer
import com.highliuk.manai.domain.ml.TokenizerResult
import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import javax.inject.Singleton

@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [FuriganaModule::class]
)
object TestFuriganaModule {

    @Provides
    @Singleton
    fun provideJapaneseTokenizer(): JapaneseTokenizer = object : JapaneseTokenizer {
        override suspend fun init() = Unit
        override fun tokenize(text: String): List<TokenizerResult> =
            listOf(TokenizerResult(surface = text, reading = null))
    }

    @Provides
    @Singleton
    fun provideKanjiReadingsDataSource(): KanjiReadingsDataSource = object : KanjiReadingsDataSource {
        override suspend fun load() = Unit
        override fun getReadings(kanji: Char): List<String> = emptyList()
    }

    @Provides
    @Singleton
    fun provideKanjiSplitter(dataSource: KanjiReadingsDataSource): KanjiSplitter =
        KanjiSplitter(dataSource)

    @Provides
    @Singleton
    fun provideFuriganaMatcher(splitter: KanjiSplitter): FuriganaMatcher =
        FuriganaMatcher(splitter)
}
