package com.highliuk.manai.ui.testutil

import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.StaleObjectException
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until
import org.junit.Assert.assertNotNull

private const val DEFAULT_TIMEOUT_MS = 5_000L

/**
 * Waits for an object matching [selector], then runs [action] on it.
 *
 * If the accessibility node backing the object is invalidated between find and interaction
 * (e.g. by a Compose recomposition), UiAutomator throws [StaleObjectException]. In that case
 * this helper waits for the UI to go idle, re-finds a fresh instance of the object and retries
 * [action] exactly once.
 *
 * @return the value produced by [action].
 */
fun <T> UiDevice.onFreshObject(
    selector: BySelector,
    timeoutMs: Long = DEFAULT_TIMEOUT_MS,
    action: (UiObject2) -> T,
): T {
    val target = wait(Until.findObject(selector), timeoutMs)
    assertNotNull("Object matching $selector should appear within ${timeoutMs}ms", target)
    return try {
        action(target)
    } catch (_: StaleObjectException) {
        waitForIdle()
        val fresh = wait(Until.findObject(selector), timeoutMs)
        assertNotNull(
            "Object matching $selector should reappear after StaleObjectException",
            fresh,
        )
        action(fresh)
    }
}

/** Waits for [selector] and clicks it, retrying once on [StaleObjectException]. */
fun UiDevice.clickOn(selector: BySelector, timeoutMs: Long = DEFAULT_TIMEOUT_MS) {
    onFreshObject(selector, timeoutMs) { it.click() }
}
