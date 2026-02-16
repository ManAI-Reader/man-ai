package com.highliuk.manai.ui.reader

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class ReaderGestureState {
    var areBarsVisible by mutableStateOf(false)
        private set

    fun toggleBars() {
        areBarsVisible = !areBarsVisible
    }
}
