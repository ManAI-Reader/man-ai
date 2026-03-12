package com.highliuk.manai.domain.usecase

import android.graphics.Bitmap
import com.highliuk.manai.domain.ml.OcrResult
import com.highliuk.manai.domain.ml.TextDetector
import com.highliuk.manai.domain.ml.TextRecognizer
import com.highliuk.manai.domain.ml.TextRegion
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test

class WarmUpOnnxUseCaseTest {

    private val textDetector = mockk<TextDetector>(relaxed = true)
    private val textRecognizer = mockk<TextRecognizer>(relaxed = true)

    private val useCase = WarmUpOnnxUseCase(textDetector, textRecognizer)

    @Before
    fun setUp() {
        mockkStatic(Bitmap::class)
        every { Bitmap.createBitmap(any<Int>(), any<Int>(), any()) } returns mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        unmockkStatic(Bitmap::class)
    }

    @Test
    fun `execute calls detect and recognize to warm up sessions`() = runTest {
        val dummyRegion = TextRegion(0f, 0f, 32f, 32f, 1f)
        coEvery { textDetector.detect(any()) } returns listOf(dummyRegion)
        coEvery { textRecognizer.recognize(any(), any()) } returns OcrResult("", dummyRegion)

        useCase.execute()

        coVerify { textDetector.detect(any()) }
        coVerify { textRecognizer.recognize(any(), any()) }
    }

    @Test
    fun `execute calls initialize on detector and recognizer`() = runTest {
        useCase.execute()

        coVerify { textDetector.initialize() }
        coVerify { textRecognizer.initialize() }
    }

    @Test
    fun `execute recycles bitmap in finally block`() = runTest {
        val dummyBitmap = mockk<Bitmap>(relaxed = true)
        every { Bitmap.createBitmap(any<Int>(), any<Int>(), any()) } returns dummyBitmap
        coEvery { textDetector.detect(any()) } returns emptyList()

        useCase.execute()

        verify { dummyBitmap.recycle() }
    }
}
