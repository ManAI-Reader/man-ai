package com.highliuk.manai.domain.furigana

import com.highliuk.manai.domain.model.FuriganaToken
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Caches furigana parsing results per Japanese run so that streaming updates
 * and recompositions never re-tokenize the same text twice.
 */
class FuriganaRunCache(
    private val parse: suspend (String) -> List<FuriganaToken>,
) {
    private val mutex = Mutex()
    private val cache = mutableMapOf<String, List<FuriganaToken>>()

    suspend fun get(run: String): List<FuriganaToken> = mutex.withLock {
        cache.getOrPut(run) { parse(run) }
    }
}
