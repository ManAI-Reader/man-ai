package com.highliuk.manai.ui.chat.markdown

import com.highliuk.manai.domain.model.FuriganaPart
import com.highliuk.manai.domain.model.FuriganaToken
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class FuriganaRunResolverTest {

    private fun token(surface: String) = FuriganaToken(
        surface = surface,
        reading = null,
        parts = listOf(FuriganaPart.kana(surface)),
    )

    @Test
    fun missingRunsAreResolvedAndCached() = runTest {
        val cache = mutableMapOf<String, List<FuriganaToken>>()

        FuriganaRunResolver.resolveMissing(listOf("漢字", "仮名"), cache) { run ->
            listOf(token(run))
        }

        assertEquals(listOf(token("漢字")), cache["漢字"])
        assertEquals(listOf(token("仮名")), cache["仮名"])
    }

    @Test
    fun cachedRunsAreNotResolvedAgain() = runTest {
        val cache = mutableMapOf("漢字" to listOf(token("漢字")))
        val resolved = mutableListOf<String>()

        FuriganaRunResolver.resolveMissing(listOf("漢字", "仮名"), cache) { run ->
            resolved.add(run)
            listOf(token(run))
        }

        assertEquals(listOf("仮名"), resolved)
    }

    @Test
    fun emptyResultsAreNotCachedSoTheRunCanBeRetried() = runTest {
        val cache = mutableMapOf<String, List<FuriganaToken>>()

        FuriganaRunResolver.resolveMissing(listOf("漢字"), cache) { emptyList() }

        assertFalse(cache.containsKey("漢字"))

        // A later pass (e.g. the next streaming emission) retries and caches.
        FuriganaRunResolver.resolveMissing(listOf("漢字"), cache) { run ->
            listOf(token(run))
        }

        assertEquals(listOf(token("漢字")), cache["漢字"])
    }
}
