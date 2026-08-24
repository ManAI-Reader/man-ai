package com.highliuk.manai.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class LlmVendorTest {

    @Test
    fun `valueOfOrDefault parses stored vendor names`() {
        assertEquals(LlmVendor.GROQ, LlmVendor.valueOfOrDefault("GROQ"))
        assertEquals(LlmVendor.DEEPSEEK, LlmVendor.valueOfOrDefault("DEEPSEEK"))
    }

    @Test
    fun `valueOfOrDefault falls back to GROQ for null or unknown names`() {
        assertEquals(LlmVendor.GROQ, LlmVendor.valueOfOrDefault(null))
        assertEquals(LlmVendor.GROQ, LlmVendor.valueOfOrDefault("BANANAS"))
        assertEquals(LlmVendor.GROQ, LlmVendor.valueOfOrDefault(""))
    }

    @Test
    fun `each vendor exposes its default model`() {
        assertEquals("openai/gpt-oss-120b", LlmVendor.GROQ.defaultModel)
        assertEquals("deepseek-v4-flash", LlmVendor.DEEPSEEK.defaultModel)
    }

    @Test
    fun `groq supports default plus the three gpt-oss efforts and cannot disable reasoning`() {
        assertEquals(
            listOf(
                ReasoningLevel.DEFAULT,
                ReasoningLevel.LOW,
                ReasoningLevel.MEDIUM,
                ReasoningLevel.HIGH,
            ),
            LlmVendor.GROQ.supportedReasoningLevels,
        )
    }

    @Test
    fun `deepseek supports off and max but not the undocumented medium`() {
        assertEquals(
            listOf(
                ReasoningLevel.DEFAULT,
                ReasoningLevel.OFF,
                ReasoningLevel.LOW,
                ReasoningLevel.HIGH,
                ReasoningLevel.MAX,
            ),
            LlmVendor.DEEPSEEK.supportedReasoningLevels,
        )
    }
}
