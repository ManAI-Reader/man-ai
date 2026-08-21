package com.highliuk.manai.data.repository

import com.highliuk.manai.R
import com.highliuk.manai.data.local.dao.PromptTemplateDao
import com.highliuk.manai.data.local.entity.PromptTemplateEntity
import com.highliuk.manai.domain.model.PromptTemplate
import com.highliuk.manai.domain.model.ReasoningLevel
import com.highliuk.manai.domain.repository.UserPreferencesRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Test

class PromptTemplateRepositoryImplTest {

    private val dao = mockk<PromptTemplateDao>(relaxed = true)
    private val userPreferences = mockk<UserPreferencesRepository>(relaxed = true)
    private val resolveString: (Int) -> String = { resId -> RESOURCES.getValue(resId) }
    private val repository = PromptTemplateRepositoryImpl(
        dao = dao,
        userPreferences = userPreferences,
        resolveString = resolveString,
    )

    @Test
    fun `observeTemplates maps entities to domain models`() = runTest {
        every { dao.observeAll() } returns flowOf(
            listOf(
                PromptTemplateEntity(
                    id = 3L,
                    name = "Custom",
                    template = "Do {text}",
                    sortOrder = 7,
                    reasoningLevel = "HIGH",
                )
            )
        )
        coEvery { dao.count() } returns 1

        val result = repository.observeTemplates().first()

        assertEquals(
            listOf(
                PromptTemplate(
                    id = 3L,
                    name = "Custom",
                    template = "Do {text}",
                    sortOrder = 7,
                    reasoningLevel = ReasoningLevel.HIGH,
                )
            ),
            result
        )
    }

    @Test
    fun `observeTemplates maps unknown stored reasoning level to DEFAULT`() = runTest {
        every { dao.observeAll() } returns flowOf(
            listOf(
                PromptTemplateEntity(
                    id = 3L,
                    name = "Custom",
                    template = "Do {text}",
                    sortOrder = 0,
                    reasoningLevel = "BANANAS",
                )
            )
        )
        coEvery { dao.count() } returns 1

        val result = repository.observeTemplates().first()

        assertEquals(ReasoningLevel.DEFAULT, result.single().reasoningLevel)
    }

    @Test
    fun `save stores reasoning level enum name on the entity`() = runTest {
        val entitySlot = slot<PromptTemplateEntity>()
        coEvery { dao.upsert(capture(entitySlot)) } returns 1L

        repository.save(
            PromptTemplate(
                id = 4L,
                name = "Deep",
                template = "Think about {text}",
                sortOrder = 1,
                reasoningLevel = ReasoningLevel.MEDIUM,
            )
        )

        assertEquals("MEDIUM", entitySlot.captured.reasoningLevel)
    }

    @Test
    fun `observeTemplates seeds two defaults when table empty and flag unset`() = runTest {
        every { dao.observeAll() } returns flowOf(emptyList())
        coEvery { dao.count() } returns 0
        every { userPreferences.promptDefaultsSeeded } returns flowOf(false)
        val inserted = mutableListOf<PromptTemplateEntity>()
        coEvery { dao.upsert(capture(inserted)) } returns 1L

        repository.observeTemplates().first()

        assertEquals(2, inserted.size)
        assertEquals("Explain this word", inserted[0].name)
        assertEquals("Word template body 「{selection}」 in 「{text}」", inserted[0].template)
        assertEquals(0, inserted[0].sortOrder)
        assertEquals("Explain grammar", inserted[1].name)
        assertEquals("Grammar template body 「{selection}」 in 「{text}」", inserted[1].template)
        assertEquals(1, inserted[1].sortOrder)
        coVerify(exactly = 1) { userPreferences.setPromptDefaultsSeeded() }
    }

    @Test
    fun `concurrent first collections seed defaults exactly once`() = runTest {
        every { dao.observeAll() } returns flowOf(emptyList())
        every { userPreferences.promptDefaultsSeeded } returns flowOf(false)
        var insertedCount = 0
        coEvery { dao.count() } coAnswers {
            val snapshot = insertedCount
            yield() // force interleaving between concurrent collectors after the read
            snapshot
        }
        coEvery { dao.upsert(any()) } coAnswers {
            insertedCount++
            1L
        }

        val first = launch { repository.observeTemplates().first() }
        val second = launch { repository.observeTemplates().first() }
        first.join()
        second.join()

        coVerify(exactly = 2) { dao.upsert(any()) }
    }

    @Test
    fun `observeTemplates does not seed when table has templates`() = runTest {
        every { dao.observeAll() } returns flowOf(emptyList())
        coEvery { dao.count() } returns 2

        repository.observeTemplates().first()

        coVerify(exactly = 0) { dao.upsert(any()) }
        coVerify(exactly = 0) { userPreferences.setPromptDefaultsSeeded() }
    }

    @Test
    fun `observeTemplates does not reseed when flag already set`() = runTest {
        every { dao.observeAll() } returns flowOf(emptyList())
        coEvery { dao.count() } returns 0
        every { userPreferences.promptDefaultsSeeded } returns flowOf(true)

        repository.observeTemplates().first()

        coVerify(exactly = 0) { dao.upsert(any()) }
        coVerify(exactly = 0) { userPreferences.setPromptDefaultsSeeded() }
    }

    @Test
    fun `save maps domain model to entity`() = runTest {
        repository.save(PromptTemplate(id = 5L, name = "N", template = "T", sortOrder = 2))

        coVerify {
            dao.upsert(PromptTemplateEntity(id = 5L, name = "N", template = "T", sortOrder = 2))
        }
    }

    @Test
    fun `delete delegates to dao`() = runTest {
        repository.delete(9L)

        coVerify { dao.delete(9L) }
    }

    private companion object {
        val RESOURCES = mapOf(
            R.string.prompt_default_word to "Explain this word",
            R.string.prompt_default_grammar to "Explain grammar",
            R.string.prompt_template_word to "Word template body 「{selection}」 in 「{text}」",
            R.string.prompt_template_grammar to "Grammar template body 「{selection}」 in 「{text}」",
        )
    }
}
