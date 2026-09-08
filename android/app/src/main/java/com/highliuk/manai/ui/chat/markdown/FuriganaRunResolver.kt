package com.highliuk.manai.ui.chat.markdown

import com.highliuk.manai.domain.model.FuriganaToken

/**
 * Fills a per-message furigana cache with the runs it is still missing.
 *
 * Empty results are never cached: they only occur on failure (or effect
 * cancellation upstream), and caching them would permanently block
 * re-resolution of the run for the rest of the message.
 */
object FuriganaRunResolver {

    suspend fun resolveMissing(
        runs: List<String>,
        cache: MutableMap<String, List<FuriganaToken>>,
        resolve: suspend (String) -> List<FuriganaToken>,
    ) {
        for (run in runs) {
            if (cache.containsKey(run)) continue
            val tokens = resolve(run)
            if (tokens.isNotEmpty()) {
                cache[run] = tokens
            }
        }
    }
}
