package com.highliuk.manai.data.pdf

import kotlinx.coroutines.delay

/**
 * Runs [block] up to [attempts] times, waiting [delayMs] between attempts,
 * until it returns a non-null result.
 *
 * PDF page rendering can fail transiently (file-descriptor pressure, races
 * while several pages render at once); retrying converts those failures into
 * recoveries instead of leaving a permanent broken-page placeholder.
 */
suspend fun <T : Any> retryRender(
    attempts: Int,
    delayMs: Long,
    block: suspend () -> T?,
): T? {
    repeat(attempts - 1) {
        block()?.let { return it }
        delay(delayMs)
    }
    return block()
}
