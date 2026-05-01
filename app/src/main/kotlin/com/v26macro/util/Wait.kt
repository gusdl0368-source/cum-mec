package com.v26macro.util

import kotlinx.coroutines.delay

/**
 * Polls [check] every [pollMs] until it returns non-null or [timeoutMs] elapses.
 * Returns the first non-null result, or null on timeout.
 */
suspend fun <T : Any> waitFor(
    timeoutMs: Long = 15_000L,
    pollMs: Long = 350L,
    check: suspend () -> T?,
): T? {
    val deadline = System.currentTimeMillis() + timeoutMs
    while (System.currentTimeMillis() < deadline) {
        check()?.let { return it }
        delay(pollMs)
    }
    return null
}

/** Sleep for a slightly randomized duration to mimic human reaction. */
suspend fun humanDelay(baseMs: Long = 600L, jitterMs: Long = 250L) {
    delay(baseMs + (Math.random() * jitterMs).toLong())
}
