package com.highliuk.manai.ui.navigation

import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

/**
 * Whether landing on [route] (a NavDestination route PATTERN, placeholders
 * included) must force the system bars back in. Only the reader manages
 * immersive mode; every other destination — and the not-yet-resolved
 * startup state — must never inherit a hidden status bar from a reader
 * left mid-transition.
 */
internal fun shouldExitImmersiveMode(route: String?): Boolean =
    route?.startsWith("reader/") != true

internal fun applyImmersiveMode(
    insetsController: WindowInsetsControllerCompat,
    immersive: Boolean
) {
    if (immersive) {
        insetsController.hide(WindowInsetsCompat.Type.systemBars())
        insetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    } else {
        insetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_DEFAULT
        insetsController.show(WindowInsetsCompat.Type.systemBars())
    }
}
