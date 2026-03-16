package com.highliuk.manai.ui.navigation

import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

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
