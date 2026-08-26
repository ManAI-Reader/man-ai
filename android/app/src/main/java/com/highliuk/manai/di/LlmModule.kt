package com.highliuk.manai.di

import android.content.SharedPreferences
import com.highliuk.manai.data.llm.LlmCredentialsManager
import com.highliuk.manai.data.llm.OpenAiCompatibleLlmProvider
import com.highliuk.manai.domain.llm.LlmProvider
import com.highliuk.manai.domain.logging.Logger
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class LlmHttpClient

@Module
@InstallIn(SingletonComponent::class)
object LlmModule {

    @Provides
    @Singleton
    @LlmHttpClient
    fun provideLlmHttpClient(): HttpClient = HttpClient(CIO) {
        engine {
            // Streaming responses stay open far longer than CIO's 15 s default.
            requestTimeout = 0
        }
    }

    @Provides
    @Singleton
    fun provideLlmCredentialsManager(
        @EncryptedPrefs prefs: SharedPreferences,
    ): LlmCredentialsManager = LlmCredentialsManager(prefs)

    @Provides
    @Singleton
    fun provideLlmProvider(
        @LlmHttpClient httpClient: HttpClient,
        credentialsManager: LlmCredentialsManager,
        logger: Logger,
    ): LlmProvider = OpenAiCompatibleLlmProvider(
        httpClient = httpClient,
        apiKeyProvider = { vendor -> credentialsManager.getApiKey(vendor).orEmpty() },
        logger = logger,
    )
}
