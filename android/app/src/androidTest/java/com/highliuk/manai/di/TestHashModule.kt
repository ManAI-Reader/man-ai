package com.highliuk.manai.di

import com.highliuk.manai.data.hash.FileHashProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import java.io.InputStream
import javax.inject.Singleton

@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [HashModule::class]
)
object TestHashModule {
    @Provides
    @Singleton
    fun provideFileHashProvider(): FileHashProvider =
        object : FileHashProvider {
            override fun computeHash(inputStream: InputStream): String = "testhash"
            override suspend fun computeHash(uri: String): String = "testhash"
        }
}
