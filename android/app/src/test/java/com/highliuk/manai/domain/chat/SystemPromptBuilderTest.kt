package com.highliuk.manai.domain.chat

import com.highliuk.manai.domain.model.TargetLanguage
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SystemPromptBuilderTest {

    @Test
    fun `prompt describes a japanese tutor inside a manga reader`() {
        val prompt = SystemPromptBuilder.build(TargetLanguage.EN)

        assertTrue(prompt.contains("Japanese language tutor"))
        assertTrue(prompt.contains("manga reader"))
    }

    @Test
    fun `prompt instructs replying in the target language display name`() {
        val english = SystemPromptBuilder.build(TargetLanguage.EN)
        val italian = SystemPromptBuilder.build(TargetLanguage.IT)

        assertTrue(english.contains("English"))
        assertTrue(italian.contains("Italiano"))
    }

    @Test
    fun `prompt lists the three memory tool names`() {
        val prompt = SystemPromptBuilder.build(TargetLanguage.EN)

        assertTrue(prompt.contains("memory_list"))
        assertTrue(prompt.contains("memory_read"))
        assertTrue(prompt.contains("memory_write"))
    }

    @Test
    fun `prompt instructs consulting memory at start and saving durable facts`() {
        val prompt = SystemPromptBuilder.build(TargetLanguage.EN)

        assertTrue(prompt.contains("At the start of a conversation"))
        assertTrue(prompt.contains("durable"))
        assertTrue(prompt.contains("save it with memory_write"))
    }

    @Test
    fun `prompt instructs using memory tools silently`() {
        val prompt = SystemPromptBuilder.build(TargetLanguage.EN)

        assertTrue(prompt.contains("Use the memory tools silently; do not narrate tool usage to the user."))
    }

    @Test
    fun `vanilla prompt keeps only the target language directive`() {
        val prompt = SystemPromptBuilder.buildVanilla(TargetLanguage.EN)

        assertTrue(prompt.contains("English"))
        assertFalse(prompt.contains("memory_"))
        assertFalse(prompt.contains("tutor"))
        assertFalse(prompt.contains("wiki"))
    }

    @Test
    fun `vanilla prompt uses the display name of each target language`() {
        assertTrue(SystemPromptBuilder.buildVanilla(TargetLanguage.IT).contains("Italiano"))
        assertTrue(SystemPromptBuilder.buildVanilla(TargetLanguage.EN).contains("English"))
    }

    @Test
    fun `vanilla prompt is a single line`() {
        assertFalse(SystemPromptBuilder.buildVanilla(TargetLanguage.EN).contains("\n"))
    }
}
