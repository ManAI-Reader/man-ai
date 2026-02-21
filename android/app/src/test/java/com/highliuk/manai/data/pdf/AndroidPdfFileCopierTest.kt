package com.highliuk.manai.data.pdf

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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayInputStream

class AndroidPdfFileCopierTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val context = mockk<Context>()
    private val contentResolver = mockk<ContentResolver>()
    private lateinit var mangaDir: java.io.File

    @Before
    fun setUp() {
        mangaDir = tempFolder.newFolder("manga")
        every { context.contentResolver } returns contentResolver
        every { context.getExternalFilesDir("manga") } returns mangaDir
        mockkStatic(Uri::class)
    }

    @After
    fun tearDown() {
        unmockkStatic(Uri::class)
    }

    private fun createCopier() = AndroidPdfFileCopier(context)

    @Test
    fun `copyToLocalStorage copies content and returns local URI`() = runTest {
        val pdfBytes = byteArrayOf(0x25, 0x50, 0x44, 0x46) // %PDF
        val sourceUri = mockk<Uri>()
        every { Uri.parse("content://external/test.pdf") } returns sourceUri
        every { contentResolver.openInputStream(sourceUri) } returns ByteArrayInputStream(pdfBytes)

        val copier = createCopier()
        val resultUri = copier.copyToLocalStorage("content://external/test.pdf")

        // Result should be a file:// URI pointing to manga dir
        assertTrue(resultUri.startsWith("file://${mangaDir.absolutePath}"))
        assertTrue(resultUri.endsWith(".pdf"))

        // Verify the file was actually written with correct content
        val localPath = resultUri.removePrefix("file://")
        val localFile = java.io.File(localPath)
        assertTrue(localFile.exists())
        assertEquals(pdfBytes.toList(), localFile.readBytes().toList())
    }

    @Test(expected = IllegalArgumentException::class)
    fun `copyToLocalStorage throws when input stream is null`() = runTest {
        val sourceUri = mockk<Uri>()
        every { Uri.parse("content://invalid") } returns sourceUri
        every { contentResolver.openInputStream(sourceUri) } returns null

        val copier = createCopier()
        copier.copyToLocalStorage("content://invalid")
    }
}
