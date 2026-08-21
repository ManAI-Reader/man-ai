package com.highliuk.manai.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class ReasoningLevelTest {

    @Test
    fun `valueOfOrDefault returns matching level for every stored name`() {
        ReasoningLevel.entries.forEach { level ->
            assertEquals(level, ReasoningLevel.valueOfOrDefault(level.name))
        }
    }

    @Test
    fun `valueOfOrDefault falls back to DEFAULT for unknown value`() {
        assertEquals(ReasoningLevel.DEFAULT, ReasoningLevel.valueOfOrDefault("ULTRA"))
        assertEquals(ReasoningLevel.DEFAULT, ReasoningLevel.valueOfOrDefault(""))
        assertEquals(ReasoningLevel.DEFAULT, ReasoningLevel.valueOfOrDefault("low"))
    }

    @Test
    fun `valueOfOrDefault falls back to DEFAULT for null`() {
        assertEquals(ReasoningLevel.DEFAULT, ReasoningLevel.valueOfOrDefault(null))
    }
}
