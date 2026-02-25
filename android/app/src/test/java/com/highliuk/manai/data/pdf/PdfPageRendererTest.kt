package com.highliuk.manai.data.pdf

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

class PdfPageRendererTest {

    private val contentResolver = mockk<ContentResolver>()
    private val renderer = PdfPageRenderer(contentResolver)

    @Before
    fun setUp() {
        mockkStatic(Uri::class)
    }

    @After
    fun tearDown() {
        unmockkStatic(Uri::class)
    }

    @Test
    fun `render returns null for invalid uri`() = runTest {
        every { Uri.parse("bad://uri") } returns mockk()
        every { contentResolver.openFileDescriptor(any(), "r") } returns null

        val result = renderer.render("bad://uri", 0)

        assertNull(result)
    }

    @Test
    fun `render returns null when exception is thrown`() = runTest {
        every { Uri.parse("throw://uri") } returns mockk()
        every { contentResolver.openFileDescriptor(any(), "r") } throws SecurityException("denied")

        val result = renderer.render("throw://uri", 0)

        assertNull(result)
    }
}
