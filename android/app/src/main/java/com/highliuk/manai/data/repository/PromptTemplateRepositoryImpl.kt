package com.highliuk.manai.data.repository

import com.highliuk.manai.R
import com.highliuk.manai.data.local.dao.PromptTemplateDao
import com.highliuk.manai.data.local.entity.PromptTemplateEntity
import com.highliuk.manai.domain.model.LlmVendor
import com.highliuk.manai.domain.model.PromptTemplate
import com.highliuk.manai.domain.model.ReasoningLevel
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
    private val resolveString: (Int) -> String,
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

    /**
     * The two seeded defaults explicitly target Groq with its default model
     * (openai/gpt-oss-120b): out of the box the app talks to Groq.
     */
    private fun defaultTemplates(): List<PromptTemplateEntity> = listOf(
        PromptTemplateEntity(
            name = resolveString(R.string.prompt_default_word),
            template = resolveString(R.string.prompt_template_word),
            sortOrder = 0,
            vendor = LlmVendor.GROQ.name,
            model = LlmVendor.GROQ.defaultModel,
        ),
        PromptTemplateEntity(
            name = resolveString(R.string.prompt_default_grammar),
            template = resolveString(R.string.prompt_template_grammar),
            sortOrder = 1,
            vendor = LlmVendor.GROQ.name,
            model = LlmVendor.GROQ.defaultModel,
        ),
    )

    private fun PromptTemplateEntity.toDomain(): PromptTemplate = PromptTemplate(
        id = id,
        name = name,
        template = template,
        sortOrder = sortOrder,
        reasoningLevel = ReasoningLevel.valueOfOrDefault(reasoningLevel),
        vendor = LlmVendor.valueOfOrDefault(vendor),
        model = model,
    )

    private fun PromptTemplate.toEntity(): PromptTemplateEntity = PromptTemplateEntity(
        id = id,
        name = name,
        template = template,
        sortOrder = sortOrder,
        reasoningLevel = reasoningLevel.name,
        vendor = vendor.name,
        model = model,
    )
}
