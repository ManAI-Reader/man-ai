package com.highliuk.manai.domain.furigana

import com.highliuk.manai.domain.model.FuriganaPart
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class KanjiSplitterTest {

    private val readingsDataSource = mockk<KanjiReadingsDataSource>()
    private lateinit var splitter: KanjiSplitter

    @Before
    fun setUp() {
        splitter = KanjiSplitter(readingsDataSource)
    }

    @Test
    fun `single kanji returns directly without combinatorial search`() {
        every { readingsDataSource.getReadings('食') } returns listOf("ショク", "ジキ", "しょく", "じき", "く", "た")

        val result = splitter.split("食", "タ")

        assertEquals(listOf(FuriganaPart.kanji("食", "た")), result)
    }

    @Test
    fun `multi-kanji with rendaku`() {
        every { readingsDataSource.getReadings('食') } returns listOf("ショク", "ジキ", "しょく", "じき", "く", "た")
        every { readingsDataSource.getReadings('堂') } returns listOf("ドウ", "どう", "トウ", "とう")

        val result = splitter.split("食堂", "ショクドウ")

        assertEquals(
            listOf(
                FuriganaPart.kanji("食", "しょく"),
                FuriganaPart.kanji("堂", "どう")
            ),
            result
        )
    }

    @Test
    fun `unknown kanji returns whole reading as fallback`() {
        every { readingsDataSource.getReadings('食') } returns listOf("ショク", "しょく")
        every { readingsDataSource.getReadings('堂') } returns emptyList()

        val result = splitter.split("食堂", "ショクドウ")

        assertEquals(listOf(FuriganaPart.kanji("食堂", "しょくどう")), result)
    }

    @Test
    fun `multi-kanji no match returns whole reading as fallback`() {
        every { readingsDataSource.getReadings('花') } returns listOf("カ", "はな", "か")
        every { readingsDataSource.getReadings('火') } returns listOf("カ", "ひ", "か")

        val result = splitter.split("花火", "ハナビ")

        assertEquals(
            listOf(
                FuriganaPart.kanji("花", "はな"),
                FuriganaPart.kanji("火", "び")
            ),
            result
        )
    }
}
