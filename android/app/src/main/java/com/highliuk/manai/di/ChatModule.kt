package com.highliuk.manai.di

import android.content.Context
import com.highliuk.manai.data.local.dao.ChatMessageDao
import com.highliuk.manai.data.local.dao.ConversationDao
import com.highliuk.manai.data.local.dao.MemoryEntryDao
import com.highliuk.manai.data.local.dao.PromptTemplateDao
import com.highliuk.manai.data.repository.ChatRepositoryImpl
import com.highliuk.manai.data.repository.MemoryRepositoryImpl
import com.highliuk.manai.data.repository.PromptTemplateRepositoryImpl
import com.highliuk.manai.domain.repository.ChatRepository
import com.highliuk.manai.domain.repository.MemoryRepository
import com.highliuk.manai.domain.repository.PromptTemplateRepository
import com.highliuk.manai.domain.repository.UserPreferencesRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

// These repositories are provided here instead of RepositoryModule because @Binds
// cannot supply their lambda parameters (clock, string resolver).
@Module
@InstallIn(SingletonComponent::class)
object ChatModule {

    @Provides
    @Singleton
    fun provideChatRepository(
        conversationDao: ConversationDao,
        chatMessageDao: ChatMessageDao,
    ): ChatRepository = ChatRepositoryImpl(
        conversationDao = conversationDao,
        chatMessageDao = chatMessageDao,
    )

    @Provides
    @Singleton
    fun providePromptTemplateRepository(
        dao: PromptTemplateDao,
        userPreferences: UserPreferencesRepository,
        @ApplicationContext context: Context,
    ): PromptTemplateRepository = PromptTemplateRepositoryImpl(
        dao = dao,
        userPreferences = userPreferences,
        resolveString = { resId -> context.getString(resId) },
    )

    @Provides
    @Singleton
    fun provideMemoryRepository(dao: MemoryEntryDao): MemoryRepository =
        MemoryRepositoryImpl(dao = dao)
}
