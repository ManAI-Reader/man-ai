package com.highliuk.manai.data.furigana

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

class KanjiReadingsDataSourceImplTest {

    @Test
    fun `parses kanji with on and kun readings`() = runTest {
        val readings = parseKanjiReadings("食,ショク,ジキ,く.う,た.べる")['食'].orEmpty()

        assertTrue("ショク" in readings)
        assertTrue("ジキ" in readings)
        assertTrue("しょく" in readings)
        assertTrue("じき" in readings)
        assertTrue("く" in readings)
        assertTrue("た" in readings)
    }

    @Test
    fun `okurigana is stripped from kun readings`() = runTest {
        val readings = parseKanjiReadings("食,く.う,た.べる")['食'].orEmpty()

        assertTrue("く" in readings)
        assertTrue("た" in readings)
        assertTrue(readings.none { it.contains(".") })
    }

    @Test
    fun `on-yomi ending in tsu adds gemination variant`() = runTest {
        val readings = parseKanjiReadings("罰,バツ,バチ")['罰'].orEmpty()

        assertTrue("バツ" in readings)
        assertTrue("バッ" in readings)
    }

    @Test
    fun `on-yomi starting with a adds wa variant`() = runTest {
        val readings = parseKanjiReadings("悪,アク")['悪'].orEmpty()

        assertTrue("アク" in readings)
        assertTrue("ワク" in readings)
    }

    @Test
    fun `dashes are stripped from readings`() = runTest {
        val readings = parseKanjiReadings("食,-かわ-")['食'].orEmpty()

        assertTrue("かわ" in readings)
        assertTrue(readings.none { it.contains("-") })
    }

    @Test
    fun `readings stored in both hiragana and katakana`() = runTest {
        val readings = parseKanjiReadings("食,ショク")['食'].orEmpty()

        assertTrue("ショク" in readings)
        assertTrue("しょく" in readings)
    }

    @Test
    fun `unknown kanji returns empty list`() = runTest {
        val parsed = parseKanjiReadings("食,ショク")

        assertTrue(parsed['堂'].isNullOrEmpty())
    }

    @Test
    fun `multiple kanji lines parsed correctly`() = runTest {
        val parsed = parseKanjiReadings("食,ショク\n堂,ドウ,トウ")

        assertTrue(parsed['食']?.isNotEmpty() == true)
        assertTrue(parsed['堂']?.isNotEmpty() == true)
    }
}
