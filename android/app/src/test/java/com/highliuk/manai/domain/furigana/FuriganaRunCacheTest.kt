package com.highliuk.manai.domain.furigana

import com.highliuk.manai.domain.model.FuriganaPart
import com.highliuk.manai.domain.model.FuriganaToken
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class FuriganaRunCacheTest {

    private fun token(surface: String) = FuriganaToken(
        surface = surface,
        reading = null,
        parts = listOf(FuriganaPart.kana(surface)),
    )

    @Test
    fun delegatesToParserAndReturnsItsResult() = runTest {
        val cache = FuriganaRunCache { text -> listOf(token(text)) }

        assertEquals(listOf(token("漢字")), cache.get("漢字"))
    }

    @Test
    fun sameRunIsParsedExactlyOnce() = runTest {
        var calls = 0
        val cache = FuriganaRunCache { text ->
            calls++
            listOf(token(text))
        }

        cache.get("漢字")
        cache.get("漢字")
        cache.get("漢字")

        assertEquals(1, calls)
    }

    @Test
    fun distinctRunsAreParsedIndependently() = runTest {
        val parsed = mutableListOf<String>()
        val cache = FuriganaRunCache { text ->
            parsed.add(text)
            listOf(token(text))
        }

        cache.get("最初")
        cache.get("最後")
        cache.get("最初")

        assertEquals(listOf("最初", "最後"), parsed)
    }

    @Test
    fun concurrentRequestsForSameRunParseOnce() = runTest {
        var calls = 0
        val cache = FuriganaRunCache { text ->
            calls++
            listOf(token(text))
        }

        val first = async { cache.get("漢字") }
        val second = async { cache.get("漢字") }
        first.await()
        second.await()

        assertEquals(1, calls)
    }
}
