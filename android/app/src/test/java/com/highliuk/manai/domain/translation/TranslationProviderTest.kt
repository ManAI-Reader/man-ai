package com.highliuk.manai.domain.translation

import com.highliuk.manai.domain.model.TranslationResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class TranslationProviderTest {

    private class FakeProvider : TranslationProvider {
        override val id: String = "fake"
        override suspend fun translate(text: String, targetLang: String): TranslationResult =
            TranslationResult.Success("translated: $text")
    }

    @Test
    fun `provider has id`() {
        val provider = FakeProvider()
        assertEquals("fake", provider.id)
    }

    @Test
    fun `translate returns result`() = runTest {
        val provider = FakeProvider()
        val result = provider.translate("hello", "EN")
        assertEquals(TranslationResult.Success("translated: hello"), result)
    }
}
