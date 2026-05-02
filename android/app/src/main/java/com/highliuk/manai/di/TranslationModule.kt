package com.highliuk.manai.di

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.highliuk.manai.data.local.dao.TranslationResultDao
import com.highliuk.manai.data.translation.DeepLCredentialsManager
import com.highliuk.manai.data.translation.DeepLTranslationProvider
import com.highliuk.manai.data.translation.TranslationRepositoryImpl
import com.highliuk.manai.domain.repository.TranslationRepository
import com.highliuk.manai.domain.repository.UserPreferencesRepository
import com.highliuk.manai.domain.translation.TranslationProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class EncryptedPrefs

@Module
@InstallIn(SingletonComponent::class)
object TranslationModule {

    @Provides
    @Singleton
    @EncryptedPrefs
    fun provideEncryptedSharedPreferences(
        @ApplicationContext context: Context,
    ): SharedPreferences {
        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        return EncryptedSharedPreferences.create(
            "deepl_prefs",
            masterKeyAlias,
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    @Provides
    @Singleton
    fun provideDeepLCredentialsManager(
        @EncryptedPrefs prefs: SharedPreferences,
    ): DeepLCredentialsManager = DeepLCredentialsManager(prefs)

    @Provides
    @Singleton
    fun provideHttpClient(): HttpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    @Provides
    @Singleton
    fun provideTranslationProvider(
        httpClient: HttpClient,
        credentialsManager: DeepLCredentialsManager,
    ): TranslationProvider = DeepLTranslationProvider(
        httpClient = httpClient,
        apiKeyProvider = { credentialsManager.getApiKey().orEmpty() },
    )

    @Provides
    @Singleton
    fun provideTranslationRepository(
        dao: TranslationResultDao,
        provider: TranslationProvider,
        userPreferencesRepository: UserPreferencesRepository,
    ): TranslationRepository = TranslationRepositoryImpl(
        dao = dao,
        provider = provider,
        targetLangFlow = userPreferencesRepository.translationTargetLang,
    )
}
