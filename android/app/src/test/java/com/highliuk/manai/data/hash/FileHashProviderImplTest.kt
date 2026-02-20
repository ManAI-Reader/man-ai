package com.highliuk.manai.data.hash

import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import java.io.ByteArrayInputStream

class FileHashProviderImplTest {

    private val provider = FileHashProviderImpl(mockk())

    @Test
    fun `computeHash returns consistent hash for same content`() {
        val content = "hello manga world".toByteArray()
        val hash1 = provider.computeHash(ByteArrayInputStream(content))
        val hash2 = provider.computeHash(ByteArrayInputStream(content))

        assertEquals(hash1, hash2)
    }

    @Test
    fun `computeHash returns different hash for different content`() {
        val hash1 = provider.computeHash(ByteArrayInputStream("file A".toByteArray()))
        val hash2 = provider.computeHash(ByteArrayInputStream("file B".toByteArray()))

        assertNotEquals(hash1, hash2)
    }

    @Test
    fun `computeHash returns hex string`() {
        val hash = provider.computeHash(ByteArrayInputStream("test".toByteArray()))

        assert(hash.matches(Regex("^[0-9a-f]+$"))) { "Hash should be lowercase hex: $hash" }
    }
}
