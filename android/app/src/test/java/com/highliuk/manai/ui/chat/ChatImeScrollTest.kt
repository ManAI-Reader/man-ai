package com.highliuk.manai.ui.chat

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatImeScrollTest {

    // shouldPinChatToBottom

    @Test
    fun pinsToBottomWhenImeOpeningAndUserWasAtBottom() {
        assertTrue(shouldPinChatToBottom(imeOpening = true, wasAtBottom = true, itemCount = 5))
    }

    @Test
    fun doesNotPinWhenImeIsNotOpening() {
        assertFalse(shouldPinChatToBottom(imeOpening = false, wasAtBottom = true, itemCount = 5))
    }

    @Test
    fun doesNotPinWhenUserWasReadingOlderMessages() {
        assertFalse(shouldPinChatToBottom(imeOpening = true, wasAtBottom = false, itemCount = 5))
    }

    @Test
    fun doesNotPinWhenListIsEmpty() {
        assertFalse(shouldPinChatToBottom(imeOpening = true, wasAtBottom = true, itemCount = 0))
    }

    // updateWasAtBottom

    @Test
    fun tracksBottomStateWhileImeIsClosed() {
        assertTrue(updateWasAtBottom(imeHeightPx = 0, atBottomNow = true, previous = false))
        assertFalse(updateWasAtBottom(imeHeightPx = 0, atBottomNow = false, previous = true))
    }

    @Test
    fun freezesBottomStateWhileImeIsVisibleOrAnimating() {
        // While the IME opens the viewport shrinks, so the live "at bottom"
        // reading becomes false even if the user was at the bottom: the
        // frozen value must win.
        assertTrue(updateWasAtBottom(imeHeightPx = 320, atBottomNow = false, previous = true))
        assertFalse(updateWasAtBottom(imeHeightPx = 320, atBottomNow = true, previous = false))
    }

    // updateImeOpening

    @Test
    fun openingStartsOnZeroToPositiveTransition() {
        assertTrue(updateImeOpening(imeHeightPx = 120, lastImeHeightPx = 0, wasOpening = false))
    }

    @Test
    fun openingContinuesWhileHeightKeepsGrowing() {
        assertTrue(updateImeOpening(imeHeightPx = 240, lastImeHeightPx = 120, wasOpening = true))
    }

    @Test
    fun openingEndsWhenHeightStabilizes() {
        assertFalse(updateImeOpening(imeHeightPx = 320, lastImeHeightPx = 320, wasOpening = true))
    }

    @Test
    fun openingDoesNotStartWhenAlreadyOpenImeGrows() {
        // A suggestion strip or keyboard-mode switch grows an already-open
        // IME; that must not re-anchor the list while the user reads older
        // messages with a stale frozen "was at bottom" flag.
        assertFalse(updateImeOpening(imeHeightPx = 380, lastImeHeightPx = 320, wasOpening = false))
    }

    @Test
    fun openingDoesNotStartWhileImeCloses() {
        assertFalse(updateImeOpening(imeHeightPx = 120, lastImeHeightPx = 320, wasOpening = true))
        assertFalse(updateImeOpening(imeHeightPx = 0, lastImeHeightPx = 320, wasOpening = false))
    }
}
