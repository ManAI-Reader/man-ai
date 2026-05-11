package com.highliuk.manai.domain.furigana

import com.highliuk.manai.domain.model.FuriganaPart
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class FuriganaMatcherTest {

    private val kanjiSplitter = mockk<KanjiSplitter>()
    private lateinit var matcher: FuriganaMatcher

    @Before
    fun setUp() {
        matcher = FuriganaMatcher(kanjiSplitter)
    }

    @Test
    fun `pure kana returns kana parts`() {
        val result = matcher.match("あいう", "アイウ")

        assertEquals(
            listOf(
                FuriganaPart.kana("あ"),
                FuriganaPart.kana("い"),
                FuriganaPart.kana("う")
            ),
            result
        )
    }

    @Test
    fun `pure kanji delegates to KanjiSplitter`() {
        val expected = listOf(
            FuriganaPart.kanji("食", "しょく"),
            FuriganaPart.kanji("堂", "どう")
        )
        every { kanjiSplitter.split("食堂", "ショクドウ") } returns expected

        val result = matcher.match("食堂", "ショクドウ")

        assertEquals(expected, result)
    }

    @Test
    fun `mixed kanji and kana splits correctly`() {
        every { kanjiSplitter.split("食", "タ") } returns listOf(
            FuriganaPart.kanji("食", "た")
        )

        val result = matcher.match("食べる", "タベル")

        assertEquals(
            listOf(
                FuriganaPart.kanji("食", "た"),
                FuriganaPart.kana("べ"),
                FuriganaPart.kana("る")
            ),
            result
        )
    }

    @Test
    fun `mixed with multiple kanji groups`() {
        every { kanjiSplitter.split("取", "ト") } returns listOf(
            FuriganaPart.kanji("取", "と")
        )
        every { kanjiSplitter.split("扱", "アツカ") } returns listOf(
            FuriganaPart.kanji("扱", "あつか")
        )

        val result = matcher.match("取り扱い", "トリアツカイ")

        assertEquals(
            listOf(
                FuriganaPart.kanji("取", "と"),
                FuriganaPart.kana("り"),
                FuriganaPart.kanji("扱", "あつか"),
                FuriganaPart.kana("い")
            ),
            result
        )
    }

    @Test
    fun `empty surface and empty reading returns empty list`() {
        val result = matcher.match("", "")

        assertEquals(emptyList<FuriganaPart>(), result)
    }

    @Test
    fun `empty surface with non-empty reading returns null`() {
        val result = matcher.match("", "ア")

        assertNull(result)
    }

    @Test
    fun `non-matching kana returns null`() {
        val result = matcher.match("あ", "カ")

        assertNull(result)
    }
}
