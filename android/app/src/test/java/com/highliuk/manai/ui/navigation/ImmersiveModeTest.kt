package com.highliuk.manai.ui.navigation

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ImmersiveModeTest {

    @Test
    fun readerRoutesKeepControlOfImmersiveMode() {
        assertFalse(shouldExitImmersiveMode("reader/{mangaId}?page={page}"))
    }

    @Test
    fun everyOtherDestinationForcesSystemBarsBackIn() {
        assertTrue(shouldExitImmersiveMode("chat/{conversationId}"))
        assertTrue(shouldExitImmersiveMode("conversations"))
        assertTrue(shouldExitImmersiveMode("home"))
        assertTrue(shouldExitImmersiveMode("settings"))
    }

    @Test
    fun missingDestinationForcesSystemBarsBackIn() {
        assertTrue(shouldExitImmersiveMode(null))
    }
}
