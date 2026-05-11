package com.highliuk.manai.domain.usecase

import com.highliuk.manai.domain.furigana.FuriganaMatcher
import com.highliuk.manai.domain.ml.JapaneseTokenizer
import com.highliuk.manai.domain.ml.TokenizerResult
import com.highliuk.manai.domain.model.FuriganaPart
import com.highliuk.manai.domain.model.FuriganaToken
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class ParseFuriganaUseCaseTest {

    private val tokenizer = mockk<JapaneseTokenizer>()
    private val matcher = mockk<FuriganaMatcher>()
    private lateinit var useCase: ParseFuriganaUseCase

    @Before
    fun setUp() {
        useCase = ParseFuriganaUseCase(tokenizer, matcher)
    }

    @Test
    fun `japanese-only text is tokenized and matched`() {
        every { tokenizer.tokenize("食べる") } returns listOf(
            TokenizerResult("食べる", "タベル")
        )
        every { matcher.match("食べる", "タベル") } returns listOf(
            FuriganaPart.kanji("食", "た"),
            FuriganaPart.kana("べ"),
            FuriganaPart.kana("る")
        )

        val result = useCase("食べる")

        assertEquals(1, result.size)
        assertEquals("食べる", result[0].surface)
        assertEquals("タベル", result[0].reading)
        assertEquals(3, result[0].parts.size)
    }

    @Test
    fun `mixed text splits into japanese and non-japanese segments`() {
        every { tokenizer.tokenize("世界") } returns listOf(
            TokenizerResult("世界", "セカイ")
        )
        every { matcher.match("世界", "セカイ") } returns listOf(
            FuriganaPart.kanji("世", "せ"),
            FuriganaPart.kanji("界", "かい")
        )

        val result = useCase("Hello世界")

        assertEquals(2, result.size)
        assertEquals("Hello", result[0].surface)
        assertEquals(listOf(FuriganaPart.kana("Hello")), result[0].parts)
        assertEquals("世界", result[1].surface)
        verify(exactly = 0) { tokenizer.tokenize("Hello") }
    }

    @Test
    fun `token with non-japanese surface passes through as kana`() {
        every { tokenizer.tokenize("テスト") } returns listOf(
            TokenizerResult("テスト", "テスト")
        )
        every { matcher.match("テスト", "テスト") } returns listOf(
            FuriganaPart.kana("テ"),
            FuriganaPart.kana("ス"),
            FuriganaPart.kana("ト")
        )

        val result = useCase("テスト")

        assertEquals(1, result.size)
        assertEquals("テスト", result[0].surface)
    }

    @Test
    fun `token with null reading passes through as kana`() {
        every { tokenizer.tokenize("ABC") } returns listOf(
            TokenizerResult("ABC", null)
        )

        val result = useCase("ABC")

        assertEquals(1, result.size)
        assertEquals("ABC", result[0].surface)
        assertEquals(listOf(FuriganaPart.kana("ABC")), result[0].parts)
    }

    @Test
    fun `token with non-katakana reading passes through as kana`() {
        every { tokenizer.tokenize("何か") } returns listOf(
            TokenizerResult("何か", "something")
        )

        val result = useCase("何か")

        assertEquals(1, result.size)
        assertEquals(listOf(FuriganaPart.kana("何か")), result[0].parts)
    }

    @Test
    fun `matcher returning null falls back to kana`() {
        every { tokenizer.tokenize("食べる") } returns listOf(
            TokenizerResult("食べる", "タベル")
        )
        every { matcher.match("食べる", "タベル") } returns null

        val result = useCase("食べる")

        assertEquals(1, result.size)
        assertEquals(listOf(FuriganaPart.kana("食べる")), result[0].parts)
    }

    @Test
    fun `empty text returns empty list`() {
        val result = useCase("")

        assertEquals(emptyList<FuriganaToken>(), result)
    }
}
