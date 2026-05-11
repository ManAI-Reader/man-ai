package com.highliuk.manai.di

import android.content.Context
import com.highliuk.manai.data.furigana.KanjiReadingsDataSourceImpl
import com.highliuk.manai.data.ml.KuromojiTokenizer
import com.highliuk.manai.domain.furigana.FuriganaMatcher
import com.highliuk.manai.domain.furigana.KanjiReadingsDataSource
import com.highliuk.manai.domain.furigana.KanjiSplitter
import com.highliuk.manai.domain.ml.JapaneseTokenizer
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object FuriganaModule {

    @Provides
    @Singleton
    fun provideJapaneseTokenizer(): JapaneseTokenizer = KuromojiTokenizer()

    @Provides
    @Singleton
    fun provideKanjiReadingsDataSource(
        @ApplicationContext context: Context,
    ): KanjiReadingsDataSource = KanjiReadingsDataSourceImpl(context)

    @Provides
    @Singleton
    fun provideKanjiSplitter(dataSource: KanjiReadingsDataSource): KanjiSplitter =
        KanjiSplitter(dataSource)

    @Provides
    @Singleton
    fun provideFuriganaMatcher(splitter: KanjiSplitter): FuriganaMatcher =
        FuriganaMatcher(splitter)
}
