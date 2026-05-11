package com.highliuk.manai.ui.reader

import com.highliuk.manai.domain.model.FuriganaPart
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RubySpanBuildingTest {

    @Test
    fun `kanji with reading produces span instruction at correct indices`() {
        val parts = listOf(
            FuriganaPart.kanji("食", "た"),
            FuriganaPart.kana("べ"),
            FuriganaPart.kana("る")
        )

        val instructions = calculateSpanInstructions(parts)

        assertEquals(1, instructions.size)
        assertEquals(SpanInstruction(start = 0, end = 1, reading = "た"), instructions[0])
    }

    @Test
    fun `parts without reading produce no span instructions`() {
        val parts = listOf(
            FuriganaPart.kana("あ"),
            FuriganaPart.kana("い")
        )

        val instructions = calculateSpanInstructions(parts)

        assertTrue(instructions.isEmpty())
    }

    @Test
    fun `empty parts list returns empty instructions`() {
        val instructions = calculateSpanInstructions(emptyList())

        assertTrue(instructions.isEmpty())
    }

    @Test
    fun `multiple kanji parts each produce their own instruction`() {
        val parts = listOf(
            FuriganaPart.kanji("食", "しょく"),
            FuriganaPart.kanji("堂", "どう")
        )

        val instructions = calculateSpanInstructions(parts)

        assertEquals(2, instructions.size)
        assertEquals(SpanInstruction(start = 0, end = 1, reading = "しょく"), instructions[0])
        assertEquals(SpanInstruction(start = 1, end = 2, reading = "どう"), instructions[1])
    }

    @Test
    fun `mixed kanji and kana have correct offsets`() {
        val parts = listOf(
            FuriganaPart.kanji("取", "と"),
            FuriganaPart.kana("り"),
            FuriganaPart.kanji("扱", "あつか"),
            FuriganaPart.kana("い")
        )

        val instructions = calculateSpanInstructions(parts)

        assertEquals(2, instructions.size)
        assertEquals(SpanInstruction(start = 0, end = 1, reading = "と"), instructions[0])
        assertEquals(SpanInstruction(start = 2, end = 3, reading = "あつか"), instructions[1])
    }
}
