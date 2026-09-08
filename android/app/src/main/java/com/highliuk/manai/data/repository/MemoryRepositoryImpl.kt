package com.highliuk.manai.data.repository

import com.highliuk.manai.data.local.dao.MemoryEntryDao
import com.highliuk.manai.data.local.entity.MemoryEntryEntity
import com.highliuk.manai.domain.repository.MemoryRepository

class MemoryRepositoryImpl(
    private val dao: MemoryEntryDao,
    private val clock: () -> Long = System::currentTimeMillis,
) : MemoryRepository {

    override suspend fun listTitles(): List<String> = dao.listTitles()

    override suspend fun read(title: String): String? = dao.get(title)?.content

    override suspend fun write(title: String, content: String) {
        dao.upsert(MemoryEntryEntity(title = title, content = content, updatedAt = clock()))
    }
}
