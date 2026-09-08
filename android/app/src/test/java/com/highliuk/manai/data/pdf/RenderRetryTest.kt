package com.highliuk.manai.data.pdf

import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RenderRetryTest {

    @Test
    fun `returns first successful result without retrying`() = runTest {
        var calls = 0

        val result = retryRender(attempts = 3, delayMs = 250L) {
            calls++
            "page"
        }

        assertEquals("page", result)
        assertEquals(1, calls)
    }

    @Test
    fun `retries on null result and returns a later success`() = runTest {
        var calls = 0

        val result = retryRender(attempts = 3, delayMs = 250L) {
            calls++
            if (calls < 3) null else "page"
        }

        assertEquals("page", result)
        assertEquals(3, calls)
    }

    @Test
    fun `returns null after exhausting all attempts`() = runTest {
        var calls = 0

        val result = retryRender<String>(attempts = 3, delayMs = 250L) {
            calls++
            null
        }

        assertNull(result)
        assertEquals(3, calls)
    }

    @Test
    fun `waits the configured delay between attempts`() = runTest {
        val result = retryRender(attempts = 3, delayMs = 250L) {
            if (currentTime >= 500L) "page" else null
        }

        assertEquals("page", result)
        assertEquals(500L, currentTime)
    }
}
