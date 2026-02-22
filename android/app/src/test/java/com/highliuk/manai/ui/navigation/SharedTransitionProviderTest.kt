package com.highliuk.manai.ui.navigation

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.ProvidableCompositionLocal
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalSharedTransitionApi::class)
class SharedTransitionProviderTest {

    @Test
    fun localSharedTransitionScopeIsProvidableCompositionLocal() {
        assertTrue(
            LocalSharedTransitionScope is ProvidableCompositionLocal<SharedTransitionScope?>
        )
    }

    @Test
    fun localAnimatedVisibilityScopeIsProvidableCompositionLocal() {
        assertTrue(
            LocalAnimatedVisibilityScope is ProvidableCompositionLocal<AnimatedVisibilityScope?>
        )
    }
}
