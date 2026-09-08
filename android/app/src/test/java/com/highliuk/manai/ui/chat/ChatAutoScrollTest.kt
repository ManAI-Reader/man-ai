package com.highliuk.manai.ui.chat

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatAutoScrollTest {

    @Test
    fun followsNewestItemWhenAnchoredAtTheBottom() {
        assertTrue(shouldFollowNewestItem(firstVisibleItemIndex = 0))
    }

    @Test
    fun followsNewestItemWhenPrependPushedTheAnchorToIndexOne() {
        // With reverseLayout a new bottom item is a PREPEND: the lazy list
        // keeps its anchor on the previously-bottom item, which is now at
        // index 1, so index 1 still means "the user was at the bottom".
        assertTrue(shouldFollowNewestItem(firstVisibleItemIndex = 1))
    }

    @Test
    fun doesNotFollowWhileUserReadsOlderMessages() {
        assertFalse(shouldFollowNewestItem(firstVisibleItemIndex = 2))
        assertFalse(shouldFollowNewestItem(firstVisibleItemIndex = 40))
    }
}
