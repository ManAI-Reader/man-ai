package com.highliuk.manai.domain.chat

import com.highliuk.manai.domain.llm.LlmToolCall
import com.highliuk.manai.domain.repository.MemoryRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MemoryToolExecutorTest {

    private val memoryRepository = mockk<MemoryRepository>()
    private val executor = MemoryToolExecutor(memoryRepository)

    @Test
    fun `memory_list returns json array of titles`() = runTest {
        coEvery { memoryRepository.listTitles() } returns listOf("Level", "Vocabulary")

        val result = executor.execute(LlmToolCall(id = "1", name = "memory_list", arguments = "{}"))

        assertEquals("""["Level","Vocabulary"]""", result)
    }

    @Test
    fun `memory_read returns page content`() = runTest {
        coEvery { memoryRepository.read("Level") } returns "JLPT N4"

        val result = executor.execute(
            LlmToolCall(id = "1", name = "memory_read", arguments = """{"title":"Level"}"""),
        )

        assertEquals("JLPT N4", result)
    }

    @Test
    fun `memory_read reports missing page`() = runTest {
        coEvery { memoryRepository.read("Ghost") } returns null

        val result = executor.execute(
            LlmToolCall(id = "1", name = "memory_read", arguments = """{"title":"Ghost"}"""),
        )

        assertEquals("No memory page titled 'Ghost'.", result)
    }

    @Test
    fun `memory_write upserts and confirms`() = runTest {
        coEvery { memoryRepository.write(any(), any()) } returns Unit

        val result = executor.execute(
            LlmToolCall(
                id = "1",
                name = "memory_write",
                arguments = """{"title":"Level","content":"JLPT N4"}""",
            ),
        )

        assertEquals("Saved.", result)
        coVerify(exactly = 1) { memoryRepository.write("Level", "JLPT N4") }
    }

    @Test
    fun `unknown tool name returns error string`() = runTest {
        val result = executor.execute(LlmToolCall(id = "1", name = "memory_erase", arguments = "{}"))

        assertEquals("Unknown tool: memory_erase", result)
    }

    @Test
    fun `malformed json arguments return error string`() = runTest {
        val result = executor.execute(
            LlmToolCall(id = "1", name = "memory_read", arguments = "not json"),
        )

        assertTrue(result.startsWith("Invalid arguments for memory_read"))
    }

    @Test
    fun `non-object json arguments return error string`() = runTest {
        val result = executor.execute(
            LlmToolCall(id = "1", name = "memory_read", arguments = "[]"),
        )

        assertTrue(result.startsWith("Invalid arguments for memory_read"))
    }

    @Test
    fun `json null value for required key returns error string`() = runTest {
        val result = executor.execute(
            LlmToolCall(id = "1", name = "memory_read", arguments = """{"title":null}"""),
        )

        assertTrue(result.startsWith("Invalid arguments for memory_read"))
    }

    @Test
    fun `missing required key returns error string`() = runTest {
        val result = executor.execute(
            LlmToolCall(id = "1", name = "memory_write", arguments = """{"title":"Level"}"""),
        )

        assertTrue(result.startsWith("Invalid arguments for memory_write"))
    }

    @Test
    fun `specs describe the three memory tools`() {
        assertEquals(
            listOf("memory_list", "memory_read", "memory_write"),
            MemoryToolExecutor.SPECS.map { it.name },
        )
    }
}
