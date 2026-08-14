package com.highliuk.manai.data.repository

import com.highliuk.manai.R
import com.highliuk.manai.data.local.dao.PromptTemplateDao
import com.highliuk.manai.data.local.entity.PromptTemplateEntity
import com.highliuk.manai.domain.model.PromptTemplate
import com.highliuk.manai.domain.repository.PromptTemplateRepository
import com.highliuk.manai.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class PromptTemplateRepositoryImpl(
    private val dao: PromptTemplateDao,
    private val userPreferences: UserPreferencesRepository,
    private val resolveName: (Int) -> String,
) : PromptTemplateRepository {

    private val seedMutex = Mutex()

    override fun observeTemplates(): Flow<List<PromptTemplate>> =
        dao.observeAll()
            .map { entities -> entities.map { it.toDomain() } }
            .onStart { seedDefaultsIfNeeded() }

    override suspend fun save(template: PromptTemplate) {
        dao.upsert(template.toEntity())
    }

    override suspend fun delete(id: Long) {
        dao.delete(id)
    }

    private suspend fun seedDefaultsIfNeeded() {
        // Fast path outside the lock; re-checked inside (double-checked) so two
        // concurrent first collections cannot both seed.
        if (!needsSeeding()) return
        seedMutex.withLock {
            if (!needsSeeding()) return
            // Upserts happen before the flag write: if we crash mid-seed the flag stays
            // unset and the count check makes the next attempt self-limiting (it only
            // skips seeding once at least one template exists).
            defaultTemplates().forEach { dao.upsert(it) }
            userPreferences.setPromptDefaultsSeeded()
        }
    }

    private suspend fun needsSeeding(): Boolean =
        dao.count() == 0 && !userPreferences.promptDefaultsSeeded.first()

    private fun defaultTemplates(): List<PromptTemplateEntity> = listOf(
        PromptTemplateEntity(
            name = resolveName(R.string.prompt_default_grammar),
            template = "Explain the grammar of this sentence from a manga:\n{text}",
            sortOrder = 0,
        ),
        PromptTemplateEntity(
            name = resolveName(R.string.prompt_default_vocabulary),
            template = "Break down the vocabulary of this manga sentence. " +
                "For each word give reading and meaning:\n{text}",
            sortOrder = 1,
        ),
        PromptTemplateEntity(
            name = resolveName(R.string.prompt_default_word),
            template = "In the sentence {text}, explain the meaning, reading and nuance of: {selection}",
            sortOrder = 2,
        ),
        PromptTemplateEntity(
            name = resolveName(R.string.prompt_default_translation_check),
            template = "Original manga sentence: {text}\nTranslation: {translation}\n" +
                "Explain how the translation maps to the original, highlighting anything non-literal.",
            sortOrder = 3,
        ),
    )

    private fun PromptTemplateEntity.toDomain(): PromptTemplate =
        PromptTemplate(id = id, name = name, template = template, sortOrder = sortOrder)

    private fun PromptTemplate.toEntity(): PromptTemplateEntity =
        PromptTemplateEntity(id = id, name = name, template = template, sortOrder = sortOrder)
}
