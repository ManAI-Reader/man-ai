package com.highliuk.manai.ui.navigation

sealed class Screen(val route: String) {
    data object Home : Screen("home")
}
