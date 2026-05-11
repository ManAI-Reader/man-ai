package com.highliuk.manai.data.ml

import com.highliuk.manai.domain.ml.TokenizerResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test

class KuromojiTokenizerTest {

    private lateinit var tokenizer: KuromojiTokenizer

    @Before
    fun setUp() = runTest {
        tokenizer = KuromojiTokenizer()
        tokenizer.init()
    }

    @Test
    fun `tokenize simple verb returns surface and reading`() {
        val results = tokenizer.tokenize("食べる")

        assertEquals(1, results.size)
        assertEquals("食べる", results[0].surface)
        assertEquals("タベル", results[0].reading)
    }

    @Test
    fun `tokenize non-japanese returns null reading`() {
        val results = tokenizer.tokenize("Hello")

        assertEquals(1, results.size)
        assertEquals("Hello", results[0].surface)
        assertNull(results[0].reading)
    }

    @Test
    fun `tokenize compound noun`() {
        val results = tokenizer.tokenize("食堂")

        assertEquals(1, results.size)
        assertEquals("食堂", results[0].surface)
        assertEquals("ショクドウ", results[0].reading)
    }

    @Test
    fun `init is idempotent - underlying Tokenizer is not recreated on repeated calls`() = runTest {
        val sut = KuromojiTokenizer()
        val field = KuromojiTokenizer::class.java.getDeclaredField("tokenizer")
            .apply { isAccessible = true }

        sut.init()
        val first = field.get(sut)
        sut.init()
        sut.init()
        val later = field.get(sut)

        assertSame(first, later)
    }
}
