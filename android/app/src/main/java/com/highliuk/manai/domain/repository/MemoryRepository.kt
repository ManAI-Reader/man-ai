package com.highliuk.manai.domain.repository

interface MemoryRepository {
    suspend fun listTitles(): List<String>
    suspend fun read(title: String): String?
    suspend fun write(title: String, content: String)
}
