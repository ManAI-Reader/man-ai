package com.highliuk.manai.data.repository

import com.highliuk.manai.data.local.dao.MemoryEntryDao
import com.highliuk.manai.data.local.entity.MemoryEntryEntity
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MemoryRepositoryImplTest {

    private val dao = mockk<MemoryEntryDao>(relaxed = true)
    private val repository = MemoryRepositoryImpl(dao = dao, clock = { FIXED_NOW })

    @Test
    fun `listTitles delegates to dao`() = runTest {
        coEvery { dao.listTitles() } returns listOf("kanji notes", "vocab")

        assertEquals(listOf("kanji notes", "vocab"), repository.listTitles())
    }

    @Test
    fun `read returns content of stored entry`() = runTest {
        coEvery { dao.get("vocab") } returns
            MemoryEntryEntity(title = "vocab", content = "remembered words", updatedAt = 1L)

        assertEquals("remembered words", repository.read("vocab"))
    }

    @Test
    fun `read returns null when entry missing`() = runTest {
        coEvery { dao.get("missing") } returns null

        assertNull(repository.read("missing"))
    }

    @Test
    fun `write upserts entry with clock timestamp`() = runTest {
        repository.write("vocab", "new content")

        coVerify {
            dao.upsert(MemoryEntryEntity(title = "vocab", content = "new content", updatedAt = FIXED_NOW))
        }
    }

    private companion object {
        const val FIXED_NOW = 1_723_600_000_000L
    }
}
