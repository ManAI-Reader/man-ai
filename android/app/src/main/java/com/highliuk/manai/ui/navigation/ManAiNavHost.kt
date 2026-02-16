package com.highliuk.manai.ui.navigation

import android.net.Uri
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.highliuk.manai.ui.home.HomeScreen
import com.highliuk.manai.ui.home.HomeViewModel
import com.highliuk.manai.ui.reader.ReaderScreen
import com.highliuk.manai.ui.reader.ReaderViewModel
import com.highliuk.manai.ui.settings.SettingsScreen
import com.highliuk.manai.ui.settings.SettingsViewModel

@Composable
fun ManAiNavHost(
    onImportClick: () -> Unit
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "home",
        enterTransition = {
            slideInHorizontally(
                initialOffsetX = { it },
                animationSpec = tween(300, easing = FastOutSlowInEasing)
            )
        },
        exitTransition = {
            slideOutHorizontally(
                targetOffsetX = { -it / 3 },
                animationSpec = tween(300, easing = FastOutSlowInEasing)
            )
        },
        popEnterTransition = {
            slideInHorizontally(
                initialOffsetX = { -it / 3 },
                animationSpec = tween(300, easing = FastOutSlowInEasing)
            )
        },
        popExitTransition = {
            slideOutHorizontally(
                targetOffsetX = { it },
                animationSpec = tween(300, easing = FastOutSlowInEasing)
            )
        }
    ) {
        composable("home") {
            val viewModel: HomeViewModel = hiltViewModel()
            val mangaList by viewModel.mangaList.collectAsState()
            val gridColumns by viewModel.gridColumns.collectAsState()

            HomeScreen(
                mangaList = mangaList,
                gridColumns = gridColumns,
                onImportClick = onImportClick,
                onSettingsClick = { navController.navigate("settings") },
                onMangaClick = { manga -> navController.navigate("reader/${manga.id}") }
            )
        }
        composable(
            "reader/{mangaId}",
            arguments = listOf(navArgument("mangaId") { type = NavType.LongType })
        ) {
            val viewModel: ReaderViewModel = hiltViewModel()
            val manga by viewModel.manga.collectAsState()
            val currentPage by viewModel.currentPage.collectAsState()
            val readingMode by viewModel.readingMode.collectAsState()

            manga?.let { m ->
                ReaderScreen(
                    manga = m,
                    currentPage = currentPage,
                    readingMode = readingMode,
                    onPageChanged = viewModel::onPageChanged,
                    onBack = { navController.popBackStack() },
                    onSettingsClick = { navController.navigate("settings") }
                )
            }
        }
        composable("settings") {
            val viewModel: SettingsViewModel = hiltViewModel()
            val gridColumns by viewModel.gridColumns.collectAsState()
            val readingMode by viewModel.readingMode.collectAsState()

            SettingsScreen(
                gridColumns = gridColumns,
                onGridColumnsChange = { viewModel.setGridColumns(it) },
                readingMode = readingMode,
                onReadingModeChange = { viewModel.setReadingMode(it) },
                onBack = { navController.popBackStack() }
            )
        }
    }
}
