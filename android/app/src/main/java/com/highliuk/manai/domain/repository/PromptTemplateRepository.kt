package com.highliuk.manai.domain.repository

import com.highliuk.manai.domain.model.PromptTemplate
import kotlinx.coroutines.flow.Flow

interface PromptTemplateRepository {
    fun observeTemplates(): Flow<List<PromptTemplate>>
    suspend fun save(template: PromptTemplate)
    suspend fun delete(id: Long)
}
