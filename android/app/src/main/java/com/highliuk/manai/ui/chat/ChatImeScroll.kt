package com.highliuk.manai.ui.chat

/**
 * Decides whether the message list must be re-anchored to its absolute
 * bottom on an IME height change: only while the keyboard is opening, only
 * if the user was already at the bottom before it started opening, and only
 * if there is anything to scroll.
 */
internal fun shouldPinChatToBottom(
    imeOpening: Boolean,
    wasAtBottom: Boolean,
    itemCount: Int,
): Boolean = imeOpening && wasAtBottom && itemCount > 0

/**
 * Tracks whether the user is at the bottom of the list, but only while the
 * IME is fully closed ([imeHeightPx] == 0). As soon as the IME starts
 * opening the viewport shrinks and the live "at bottom" reading flips to
 * false even when the user never scrolled away, so the last value observed
 * with the IME closed is frozen and reused for the whole open animation.
 */
internal fun updateWasAtBottom(
    imeHeightPx: Int,
    atBottomNow: Boolean,
    previous: Boolean,
): Boolean = if (imeHeightPx == 0) atBottomNow else previous

/**
 * Tracks whether the current IME height change belongs to an opening
 * animation the list should follow. A session starts only on a
 * zero-to-positive transition and lasts while the height keeps growing;
 * growth of an already-open IME (suggestion strip appearing, keyboard mode
 * switch) is NOT an opening — by then the frozen "was at bottom" flag may
 * be stale and re-anchoring would yank the user away from older messages.
 */
internal fun updateImeOpening(
    imeHeightPx: Int,
    lastImeHeightPx: Int,
    wasOpening: Boolean,
): Boolean = when {
    lastImeHeightPx == 0 && imeHeightPx > 0 -> true
    wasOpening && imeHeightPx > lastImeHeightPx -> true
    else -> false
}
