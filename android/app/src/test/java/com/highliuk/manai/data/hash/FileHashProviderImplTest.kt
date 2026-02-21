package com.highliuk.manai.data.hash

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayInputStream

class FileHashProviderImplTest {

    private val contentResolver = mockk<ContentResolver>()
    private val context = mockk<Context> {
        every { this@mockk.contentResolver } returns this@FileHashProviderImplTest.contentResolver
    }
    private val provider = FileHashProviderImpl(context)

    @Before
    fun setUp() {
        mockkStatic(Uri::class)
    }

    @After
    fun tearDown() {
        unmockkStatic(Uri::class)
    }

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

    @Test
    fun `computeHash with uri opens stream and computes hash`() = runTest {
        val content = "manga content".toByteArray()
        val mockUri = mockk<Uri>()
        every { Uri.parse("content://test/file.pdf") } returns mockUri
        every { contentResolver.openInputStream(mockUri) } returns ByteArrayInputStream(content)

        val expectedHash = provider.computeHash(ByteArrayInputStream(content))
        val result = provider.computeHash("content://test/file.pdf")

        assertEquals(expectedHash, result)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `computeHash with uri throws when stream is null`() = runTest {
        val mockUri = mockk<Uri>()
        every { Uri.parse("content://invalid") } returns mockUri
        every { contentResolver.openInputStream(mockUri) } returns null

        provider.computeHash("content://invalid")
    }
}
