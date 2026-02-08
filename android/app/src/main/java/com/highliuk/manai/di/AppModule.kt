package com.highliuk.manai.di

import com.highliuk.manai.data.repository.MangaRepositoryImpl
import com.highliuk.manai.domain.repository.MangaRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {
    @Binds
    @Singleton
    abstract fun bindMangaRepository(impl: MangaRepositoryImpl): MangaRepository
}
