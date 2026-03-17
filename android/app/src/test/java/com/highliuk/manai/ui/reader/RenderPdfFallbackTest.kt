package com.highliuk.manai.ui.reader

import android.content.ContentResolver
import android.net.Uri
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class RenderPdfFallbackTest {

    private val contentResolver = mockk<ContentResolver>()

    @Before
    fun setUp() {
        mockkStatic(Uri::class)
    }

    @After
    fun tearDown() {
        unmockkStatic(Uri::class)
    }

    @Test
    fun `returns null when file descriptor is null`() = runTest {
        every { Uri.parse("bad://uri") } returns mockk()
        every { contentResolver.openFileDescriptor(any(), "r") } returns null

        val result = renderPdfFallback(contentResolver, "bad://uri", 0)

        assertNull(result)
    }

    @Test
    fun `returns null when exception is thrown`() = runTest {
        every { Uri.parse("throw://uri") } returns mockk()
        every { contentResolver.openFileDescriptor(any(), "r") } throws SecurityException("denied")

        val result = renderPdfFallback(contentResolver, "throw://uri", 0)

        assertNull(result)
    }
}
