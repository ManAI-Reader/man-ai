package com.highliuk.manai.domain.model

enum class ThemeMode {
    LIGHT,
    DARK,
    SYSTEM,
}

fun ThemeMode.isDark(): Boolean? = when (this) {
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
    ThemeMode.SYSTEM -> null
}
