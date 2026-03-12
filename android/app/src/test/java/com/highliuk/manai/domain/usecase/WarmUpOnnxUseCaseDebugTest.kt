package com.highliuk.manai.domain.usecase

import android.graphics.Bitmap
import app.cash.turbine.test
import com.highliuk.manai.domain.debug.DebugMlEvent
import com.highliuk.manai.domain.debug.DebugMlEventHolder
import com.highliuk.manai.domain.ml.TextDetector
import com.highliuk.manai.domain.ml.TextRecognizer
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class WarmUpOnnxUseCaseDebugTest {

    private val textDetector = mockk<TextDetector>(relaxed = true)
    private val textRecognizer = mockk<TextRecognizer>(relaxed = true)
    private val eventHolder = DebugMlEventHolder()

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
    fun `emits loading and ready events for both models`() = runTest {
        val useCase = WarmUpOnnxUseCase(textDetector, textRecognizer, eventHolder)

        eventHolder.events.test {
            useCase.execute()

            assertEquals(DebugMlEvent.ModelLoading("detector"), awaitItem())
            assertEquals(DebugMlEvent.ModelReady("detector"), awaitItem())
            assertEquals(DebugMlEvent.ModelLoading("recognizer"), awaitItem())
            assertEquals(DebugMlEvent.ModelReady("recognizer"), awaitItem())
        }
    }
}
