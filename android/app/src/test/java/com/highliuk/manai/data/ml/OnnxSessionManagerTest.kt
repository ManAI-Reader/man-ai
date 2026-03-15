package com.highliuk.manai.data.ml

import android.content.Context
import android.content.res.AssetManager
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.io.ByteArrayInputStream
import java.io.File
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class OnnxSessionManagerTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var context: Context
    private lateinit var assets: AssetManager
    private lateinit var manager: OnnxSessionManager

    @Before
    fun setUp() {
        assets = mockk()
        context = mockk()
        every { context.filesDir } returns tempFolder.root
        every { context.assets } returns assets
        manager = OnnxSessionManager(context)
    }

    @Test
    fun `getModelPath returns correct path in models directory`() {
        assertEquals(
            "${tempFolder.root.absolutePath}/models/encoder.onnx",
            manager.getModelPath("encoder.onnx"),
        )
    }

    @Test
    fun `getModelPath works for different model names`() {
        assertEquals(
            "${tempFolder.root.absolutePath}/models/decoder_first.onnx",
            manager.getModelPath("decoder_first.onnx"),
        )
    }

    @Test
    fun `copyAssetToDisk copies file from assets to models directory`() {
        val modelContent = byteArrayOf(1, 2, 3, 4, 5)
        every { assets.open("models/test.onnx") } returns ByteArrayInputStream(modelContent)

        val path = manager.copyAssetToDisk("test.onnx")

        val outFile = File(path)
        assertTrue(outFile.exists())
        assertArrayEquals(modelContent, outFile.readBytes())
        assertEquals(File(tempFolder.root, "models/test.onnx").absolutePath, path)
    }

    @Test
    fun `copyAssetToDisk skips copy when file already exists`() {
        val modelsDir = File(tempFolder.root, "models").apply { mkdirs() }
        val existing = File(modelsDir, "cached.onnx")
        existing.writeBytes(byteArrayOf(10, 20, 30))

        val path = manager.copyAssetToDisk("cached.onnx")

        assertEquals(existing.absolutePath, path)
        assertArrayEquals(byteArrayOf(10, 20, 30), File(path).readBytes())
        verify(exactly = 0) { assets.open(any()) }
    }

    @Test
    fun `copyAssetToDisk creates models directory if missing`() {
        val modelContent = byteArrayOf(99)
        every { assets.open("models/new.onnx") } returns ByteArrayInputStream(modelContent)

        manager.copyAssetToDisk("new.onnx")

        assertTrue(File(tempFolder.root, "models").isDirectory)
    }
}
