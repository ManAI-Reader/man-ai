package com.highliuk.manai.di

import com.highliuk.manai.data.hash.FileHashProvider
import com.highliuk.manai.data.hash.FileHashProviderImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class HashModule {
    @Binds
    @Singleton
    abstract fun bindFileHashProvider(impl: FileHashProviderImpl): FileHashProvider
}
