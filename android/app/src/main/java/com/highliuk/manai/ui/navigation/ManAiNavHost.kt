package com.highliuk.manai.ui.navigation

import android.app.Activity
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.highliuk.manai.ui.home.DeleteMangaDialog
import com.highliuk.manai.ui.home.HomeScreen
import com.highliuk.manai.ui.home.HomeViewModel
import com.highliuk.manai.ui.reader.ReaderScreen
import com.highliuk.manai.ui.reader.ReaderViewModel
import com.highliuk.manai.ui.settings.SettingsScreen
import com.highliuk.manai.ui.settings.SettingsViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun ManAiNavHost(
    onImportClick: () -> Unit,
    navigateToReader: SharedFlow<Long> = MutableSharedFlow(),
    hasIntentPdf: Boolean = false
) {
    val navController = rememberNavController()
    val isFromIntent = remember { hasIntentPdf }
    val startDestination = if (isFromIntent) "intent-loading" else "home"

    LaunchedEffect(navigateToReader) {
        navigateToReader.collect { mangaId ->
            navController.navigate("reader/$mangaId") {
                popUpTo(startDestination) { inclusive = isFromIntent }
            }
        }
    }

    SharedTransitionLayout(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
    ) {
        CompositionLocalProvider(LocalSharedTransitionScope provides this) {
            NavHost(
                navController = navController,
                startDestination = startDestination,
                enterTransition = { EnterTransition.None },
                exitTransition = { ExitTransition.None },
                popEnterTransition = { EnterTransition.None },
                popExitTransition = { ExitTransition.None }
            ) {
                composable("intent-loading") {
                    Box(
                        modifier = Modifier.fillMaxSize().testTag("intent_loading"),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                composable(
                    "home",
                    enterTransition = {
                        if (initialState.destination.route == "settings") {
                            slideInHorizontally(
                                initialOffsetX = { -it / 3 },
                                animationSpec = tween(300, easing = FastOutSlowInEasing)
                            )
                        } else {
                            EnterTransition.None
                        }
                    },
                    exitTransition = {
                        if (targetState.destination.route == "settings") {
                            slideOutHorizontally(
                                targetOffsetX = { -it / 3 },
                                animationSpec = tween(300, easing = FastOutSlowInEasing)
                            )
                        } else {
                            ExitTransition.None
                        }
                    },
                    popEnterTransition = { EnterTransition.None },
                    popExitTransition = { ExitTransition.None }
                ) {
                    CompositionLocalProvider(LocalAnimatedVisibilityScope provides this@composable) {
                        val viewModel: HomeViewModel = hiltViewModel()
                        val mangaList by viewModel.mangaList.collectAsState()
                        val gridColumns by viewModel.gridColumns.collectAsState()
                        val selectedMangaIds by viewModel.selectedMangaIds.collectAsState()
                        val isSelectionMode by viewModel.isSelectionMode.collectAsState()
                        val showDeleteDialog by viewModel.showDeleteDialog.collectAsState()

                        HomeScreen(
                            mangaList = mangaList,
                            gridColumns = gridColumns,
                            selectedMangaIds = selectedMangaIds,
                            isSelectionMode = isSelectionMode,
                            onImportClick = onImportClick,
                            onSettingsClick = { navController.navigate("settings") },
                            onMangaClick = { manga -> navController.navigate("reader/${manga.id}") },
                            onToggleSelection = viewModel::toggleSelection,
                            onDeleteClick = viewModel::requestDelete,
                            onClearSelection = viewModel::clearSelection
                        )

                        if (showDeleteDialog) {
                            DeleteMangaDialog(
                                mangaCount = selectedMangaIds.size,
                                onConfirm = viewModel::confirmDelete,
                                onDismiss = viewModel::dismissDelete
                            )
                        }
                    }
                }
                composable(
                    "reader/{mangaId}",
                    arguments = listOf(navArgument("mangaId") { type = NavType.LongType }),
                    enterTransition = { fadeIn(tween(300)) },
                    exitTransition = { fadeOut(tween(300)) },
                    popEnterTransition = { fadeIn(tween(300)) },
                    popExitTransition = { fadeOut(tween(300)) },
                ) {
                    CompositionLocalProvider(LocalAnimatedVisibilityScope provides this@composable) {
                        val viewModel: ReaderViewModel = hiltViewModel()
                        val manga by viewModel.manga.collectAsState()
                        val currentPage by viewModel.currentPage.collectAsState()
                        val readingMode by viewModel.readingMode.collectAsState()

                        val view = LocalView.current
                        val window = (view.context as Activity).window
                        val insetsController = WindowCompat.getInsetsController(window, view)

                        manga?.let { m ->
                            ReaderScreen(
                                manga = m,
                                currentPage = currentPage,
                                readingMode = readingMode,
                                onPageChanged = viewModel::onPageChanged,
                                onBack = {
                                    if (!navController.popBackStack()) {
                                        (view.context as? Activity)?.finish()
                                    }
                                },
                                onSettingsClick = { navController.navigate("settings") },
                                onImmersiveModeChange = { immersive ->
                                    if (immersive) {
                                        insetsController.hide(WindowInsetsCompat.Type.statusBars())
                                        insetsController.systemBarsBehavior =
                                            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                                    } else {
                                        insetsController.show(WindowInsetsCompat.Type.statusBars())
                                    }
                                }
                            )
                        }
                    }
                }
                composable(
                    "settings",
                    enterTransition = {
                        slideInHorizontally(
                            initialOffsetX = { it },
                            animationSpec = tween(300, easing = FastOutSlowInEasing)
                        )
                    },
                    exitTransition = {
                        slideOutHorizontally(
                            targetOffsetX = { it },
                            animationSpec = tween(300, easing = FastOutSlowInEasing)
                        )
                    },
                    popEnterTransition = {
                        slideInHorizontally(
                            initialOffsetX = { it },
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
                    val viewModel: SettingsViewModel = hiltViewModel()
                    val gridColumns by viewModel.gridColumns.collectAsState()
                    val readingMode by viewModel.readingMode.collectAsState()
                    val themeMode by viewModel.themeMode.collectAsState()
                    val appLanguage by viewModel.appLanguage.collectAsState()

                    SettingsScreen(
                        gridColumns = gridColumns,
                        onGridColumnsChange = { viewModel.setGridColumns(it) },
                        readingMode = readingMode,
                        onReadingModeChange = { viewModel.setReadingMode(it) },
                        themeMode = themeMode,
                        onThemeModeChange = { viewModel.setThemeMode(it) },
                        appLanguage = appLanguage,
                        onAppLanguageChange = { language ->
                            viewModel.setAppLanguage(language)
                            val locales = if (language.tag != null) {
                                LocaleListCompat.forLanguageTags(language.tag)
                            } else {
                                LocaleListCompat.getEmptyLocaleList()
                            }
                            AppCompatDelegate.setApplicationLocales(locales)
                        },
                        onBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}
