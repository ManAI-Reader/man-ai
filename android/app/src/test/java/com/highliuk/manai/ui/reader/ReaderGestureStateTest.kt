package com.highliuk.manai.ui.reader

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReaderGestureStateTest {

    @Test
    fun `bars are hidden by default`() {
        val state = ReaderGestureState()
        assertFalse(state.areBarsVisible)
    }

    @Test
    fun `toggleBars shows bars when hidden`() {
        val state = ReaderGestureState()
        state.toggleBars()
        assertTrue(state.areBarsVisible)
    }

    @Test
    fun `toggleBars hides bars when visible`() {
        val state = ReaderGestureState()
        state.toggleBars()
        state.toggleBars()
        assertFalse(state.areBarsVisible)
    }
}
