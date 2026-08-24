package com.highliuk.manai.ui.navigation

import android.app.Activity
import android.content.res.Configuration
import androidx.activity.compose.BackHandler
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.highliuk.manai.ui.home.DeleteMangaDialog
import com.highliuk.manai.ui.home.HomeScreen
import com.highliuk.manai.ui.home.HomeViewModel
import com.highliuk.manai.ui.home.RenameMangaDialog
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import com.highliuk.manai.BuildConfig
import com.highliuk.manai.R
import com.highliuk.manai.domain.model.PageRegion
import com.highliuk.manai.domain.model.PromptTemplate
import com.highliuk.manai.ui.chat.ChatLaunchOptions
import com.highliuk.manai.ui.chat.ChatLauncherViewModel
import com.highliuk.manai.ui.chat.ChatScreen
import com.highliuk.manai.ui.chat.ChatViewModel
import com.highliuk.manai.ui.chat.ConversationListScreen
import com.highliuk.manai.ui.chat.ConversationListViewModel
import com.highliuk.manai.ui.prompts.PromptEditScreen
import com.highliuk.manai.ui.prompts.PromptEditViewModel
import com.highliuk.manai.ui.prompts.PromptListScreen
import com.highliuk.manai.ui.prompts.PromptListViewModel
import com.highliuk.manai.ui.reader.ReaderScreen
import com.highliuk.manai.ui.reader.ReaderViewModel
import com.highliuk.manai.ui.settings.SettingsScreen
import com.highliuk.manai.ui.settings.SettingsViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalComposeUiApi::class)
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
        modifier = Modifier
            .fillMaxSize()
            .semantics { testTagsAsResourceId = true }
            .background(MaterialTheme.colorScheme.background)
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
                    popEnterTransition = {
                        if (initialState.destination.route?.startsWith("reader") == true) {
                            fadeIn(tween(300))
                        } else {
                            EnterTransition.None
                        }
                    },
                    popExitTransition = { ExitTransition.None }
                ) {
                    CompositionLocalProvider(LocalAnimatedVisibilityScope provides this@composable) {
                        val viewModel: HomeViewModel = hiltViewModel()
                        val mangaList by viewModel.mangaList.collectAsState()
                        val gridColumns by viewModel.gridColumns.collectAsState()
                        val gridColumnsLandscape by viewModel.gridColumnsLandscape.collectAsState()
                        val configuration = LocalConfiguration.current
                        val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
                        val selectedMangaIds by viewModel.selectedMangaIds.collectAsState()
                        val isSelectionMode by viewModel.isSelectionMode.collectAsState()
                        val showDeleteDialog by viewModel.showDeleteDialog.collectAsState()
                        val showRenameDialog by viewModel.showRenameDialog.collectAsState()
                        val renamingMangaId by viewModel.renamingMangaId.collectAsState()

                        HomeScreen(
                            mangaList = mangaList,
                            gridColumns = if (isLandscape) gridColumnsLandscape else gridColumns,
                            selectedMangaIds = selectedMangaIds,
                            isSelectionMode = isSelectionMode,
                            onImportClick = onImportClick,
                            onSettingsClick = { navController.navigate("settings") },
                            onConversationsClick = { navController.navigate("conversations") },
                            onMangaClick = { manga -> navController.navigate("reader/${manga.id}") },
                            onToggleSelection = viewModel::toggleSelection,
                            onRenameClick = viewModel::requestRename,
                            onDeleteClick = viewModel::requestDelete,
                            onClearSelection = viewModel::clearSelection
                        )

                        if (showRenameDialog) {
                            val currentTitle = mangaList
                                .firstOrNull { it.id == renamingMangaId }
                                ?.title.orEmpty()
                            RenameMangaDialog(
                                currentTitle = currentTitle,
                                onConfirm = viewModel::confirmRename,
                                onDismiss = viewModel::dismissRename
                            )
                        }

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
                    "reader/{mangaId}?page={page}",
                    arguments = listOf(
                        navArgument("mangaId") { type = NavType.LongType },
                        navArgument("page") {
                            type = NavType.IntType
                            defaultValue = -1
                        },
                    ),
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
                        val regions by viewModel.currentPageRegions.collectAsState()
                        val selectedRegion by viewModel.selectedRegion.collectAsState()
                        val ocrFontScale by viewModel.ocrFontScale.collectAsState()
                        val debugPipelineStates by viewModel.debugPipelineStates.collectAsState()
                        val tapToNavigatePortrait by viewModel.tapToNavigatePortrait.collectAsState()
                        val tapToNavigateLandscape by viewModel.tapToNavigateLandscape.collectAsState()
                        val tapToNavigate = resolveTapToNavigate(
                            LocalConfiguration.current.orientation,
                            tapToNavigatePortrait,
                            tapToNavigateLandscape,
                        )
                        val visiblePagesRegions by viewModel.visiblePagesRegions.collectAsState()
                        val translationState by viewModel.translationState.collectAsStateWithLifecycle()
                        val furiganaTokens by viewModel.furiganaTokens.collectAsState()

                        val chatLauncher: ChatLauncherViewModel = hiltViewModel()
                        val promptTemplates by chatLauncher.promptTemplates
                            .collectAsStateWithLifecycle()
                        val noPageBalloonsFallback = stringResource(R.string.no_page_balloons)
                        val noPreviousBalloonsFallback =
                            stringResource(R.string.no_previous_balloons)
                        LaunchedEffect(chatLauncher) {
                            chatLauncher.navigateToChat.collect { id ->
                                navController.navigate("chat/$id")
                            }
                        }

                        if (BuildConfig.DEBUG_ML) {
                            val context = LocalContext.current
                            LaunchedEffect(Unit) {
                                viewModel.debugEvents.collect { event ->
                                    Toast.makeText(context, event.toastMessage, Toast.LENGTH_SHORT)
                                        .show()
                                }
                            }
                        }

                        val view = LocalView.current
                        val window = (view.context as Activity).window
                        val insetsController = WindowCompat.getInsetsController(window, view)

                        BackHandler {
                            applyImmersiveMode(insetsController, false)
                            if (!navController.popBackStack()) {
                                (view.context as? Activity)?.finish()
                            }
                        }

                        manga?.let { m ->
                            ReaderScreen(
                                manga = m,
                                currentPage = currentPage,
                                readingMode = readingMode,
                                regions = regions,
                                selectedRegion = selectedRegion,
                                ocrFontScale = ocrFontScale,
                                tapToNavigate = tapToNavigate,
                                onPageChanged = viewModel::onPageChanged,
                                onRegionTapped = viewModel::onRegionTapped,
                                onDismissBottomSheet = viewModel::dismissBottomSheet,
                                onBack = {
                                    if (!navController.popBackStack()) {
                                        (view.context as? Activity)?.finish()
                                    }
                                },
                                onSettingsClick = { navController.navigate("settings") },
                                onConversationsClick = { navController.navigate("conversations") },
                                onImmersiveModeChange = { immersive ->
                                    applyImmersiveMode(insetsController, immersive)
                                },
                                debugPipelineStates = if (BuildConfig.DEBUG_ML) debugPipelineStates else emptyMap(),
                                visiblePagesRegions = visiblePagesRegions,
                                onVisiblePagesChanged = viewModel::onVisiblePagesChanged,
                                furiganaTokens = furiganaTokens,
                                translationState = translationState,
                                onTranslateClick = { viewModel.translateSelectedRegion() },
                                promptTemplates = promptTemplates,
                                onPromptWithSelection = { template, selection ->
                                    chatLauncher.launchPromptConversation(
                                        template = template,
                                        region = selectedRegion,
                                        mangaId = m.id,
                                        options = ChatLaunchOptions(
                                            selection = selection,
                                            translation = (
                                                translationState as?
                                                    ReaderViewModel.TranslationState.Translated
                                                )?.text,
                                            noPageBalloonsFallback = noPageBalloonsFallback,
                                            noPreviousBalloonsFallback = noPreviousBalloonsFallback,
                                        ),
                                    )
                                },
                            )
                        }
                    }
                }
                chatDestination(navController)
                conversationListDestination(navController)
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
                    val gridColumnsLandscape by viewModel.gridColumnsLandscape.collectAsState()
                    val readingMode by viewModel.readingMode.collectAsState()
                    val themeMode by viewModel.themeMode.collectAsState()
                    val appLanguage by viewModel.appLanguage.collectAsState()
                    val ocrFontScale by viewModel.ocrFontScale.collectAsState()
                    val tapToNavigatePortrait by viewModel.tapToNavigatePortrait.collectAsState()
                    val tapToNavigateLandscape by viewModel.tapToNavigateLandscape.collectAsState()
                    val deeplApiKey by viewModel.deeplApiKey.collectAsState()
                    val translationTargetLang by viewModel.translationTargetLang.collectAsState()
                    val showFurigana by viewModel.showFurigana.collectAsState()
                    val groqApiKey by viewModel.groqApiKey.collectAsState()
                    val deepseekApiKey by viewModel.deepseekApiKey.collectAsState()

                    SettingsScreen(
                        gridColumns = gridColumns,
                        onGridColumnsChange = { viewModel.setGridColumns(it) },
                        gridColumnsLandscape = gridColumnsLandscape,
                        onGridColumnsLandscapeChange = { viewModel.setGridColumnsLandscape(it) },
                        readingMode = readingMode,
                        onReadingModeChange = { viewModel.setReadingMode(it) },
                        themeMode = themeMode,
                        onThemeModeChange = { viewModel.setThemeMode(it) },
                        appLanguage = appLanguage,
                        comicTextScale = ocrFontScale,
                        onComicTextScaleChange = { viewModel.setOcrFontScale(it) },
                        onAppLanguageChange = { language ->
                            viewModel.setAppLanguage(language)
                            val locales = if (language.tag != null) {
                                LocaleListCompat.forLanguageTags(language.tag)
                            } else {
                                LocaleListCompat.getEmptyLocaleList()
                            }
                            AppCompatDelegate.setApplicationLocales(locales)
                        },
                        tapToNavigatePortrait = tapToNavigatePortrait,
                        onTapToNavigatePortraitChange = { viewModel.setTapToNavigatePortrait(it) },
                        tapToNavigateLandscape = tapToNavigateLandscape,
                        onTapToNavigateLandscapeChange = { viewModel.setTapToNavigateLandscape(it) },
                        showFurigana = showFurigana,
                        onShowFuriganaChange = viewModel::setShowFurigana,
                        deeplApiKey = deeplApiKey,
                        onDeeplApiKeyChange = viewModel::setDeeplApiKey,
                        translationTargetLang = translationTargetLang,
                        onTranslationTargetLangChange = viewModel::setTranslationTargetLang,
                        groqApiKey = groqApiKey,
                        onGroqApiKeyChange = viewModel::setGroqApiKey,
                        deepseekApiKey = deepseekApiKey,
                        onDeepseekApiKeyChange = viewModel::setDeepseekApiKey,
                        onManagePromptsClick = { navController.navigate("prompts") },
                        onBack = { navController.popBackStack() }
                    )
                }
                composable(
                    "prompts",
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
                    val viewModel: PromptListViewModel = hiltViewModel()
                    val templates by viewModel.templates.collectAsState()
                    val pendingDelete by viewModel.pendingDelete.collectAsState()

                    PromptListScreen(
                        templates = templates,
                        pendingDelete = pendingDelete,
                        onAddClick = {
                            navController.navigate(
                                "prompts/edit?id=${PromptEditViewModel.NEW_TEMPLATE_ID}"
                            )
                        },
                        onEditClick = { template ->
                            navController.navigate("prompts/edit?id=${template.id}")
                        },
                        onDeleteClick = viewModel::requestDelete,
                        onConfirmDelete = viewModel::confirmDelete,
                        onDismissDelete = viewModel::dismissDelete,
                        onBack = { navController.popBackStack() }
                    )
                }
                composable(
                    "prompts/edit?id={id}",
                    arguments = listOf(
                        navArgument("id") {
                            type = NavType.LongType
                            defaultValue = PromptEditViewModel.NEW_TEMPLATE_ID
                        },
                    ),
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
                    val viewModel: PromptEditViewModel = hiltViewModel()
                    val template by viewModel.template.collectAsState()
                    val editError by viewModel.editError.collectAsState()

                    LaunchedEffect(viewModel) {
                        viewModel.saved.collect {
                            navController.popBackStack()
                        }
                    }

                    PromptEditScreen(
                        template = template,
                        errorRes = editError,
                        onSave = viewModel::save,
                        modelForVendorChange = viewModel::modelForVendorChange,
                        reasoningForVendorChange = viewModel::reasoningForVendorChange,
                        onBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}

private fun NavGraphBuilder.chatDestination(navController: NavHostController) {
    composable(
        "chat/{conversationId}",
        arguments = listOf(navArgument("conversationId") { type = NavType.LongType }),
        enterTransition = { fadeIn(tween(300)) },
        exitTransition = { fadeOut(tween(300)) },
        popEnterTransition = { fadeIn(tween(300)) },
        popExitTransition = { fadeOut(tween(300)) },
    ) {
        val viewModel: ChatViewModel = hiltViewModel()
        val conversation by viewModel.conversation.collectAsStateWithLifecycle()
        val messages by viewModel.messages.collectAsStateWithLifecycle()
        val streamingText by viewModel.streamingText.collectAsStateWithLifecycle()
        val isGenerating by viewModel.isGenerating.collectAsStateWithLifecycle()
        val error by viewModel.error.collectAsStateWithLifecycle()
        val truncated by viewModel.truncated.collectAsStateWithLifecycle()

        LaunchedEffect(viewModel) {
            viewModel.deleted.collect {
                navController.popBackStack()
            }
        }

        ChatScreen(
            conversation = conversation,
            messages = messages,
            streamingText = streamingText,
            isGenerating = isGenerating,
            error = error,
            truncated = truncated,
            onSend = viewModel::sendMessage,
            onRetry = viewModel::retry,
            onOpenSourcePage = {
                conversation?.let { c ->
                    if (c.mangaId != null && c.pageIndex != null) {
                        // Plain push: popping up to the reader route here would drop the
                        // chat (and any previous reader) from the back stack, sending the
                        // user Home instead of back to the conversation.
                        navController.navigate("reader/${c.mangaId}?page=${c.pageIndex}")
                    }
                }
            },
            onDeleteConversation = viewModel::deleteConversation,
            onBack = { navController.popBackStack() },
            resolveFurigana = viewModel::resolveFurigana,
        )
    }
}

private fun NavGraphBuilder.conversationListDestination(navController: NavHostController) {
    composable(
        "conversations",
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
        val viewModel: ConversationListViewModel = hiltViewModel()
        val conversations by viewModel.conversations.collectAsState()
        val pendingDelete by viewModel.pendingDelete.collectAsState()
        val isSearchActive by viewModel.isSearchActive.collectAsState()
        val searchQuery by viewModel.searchQuery.collectAsState()

        ConversationListScreen(
            conversations = conversations,
            pendingDelete = pendingDelete,
            isSearchActive = isSearchActive,
            searchQuery = searchQuery,
            onConversationClick = { id -> navController.navigate("chat/$id") },
            onDeleteClick = viewModel::requestDelete,
            onConfirmDelete = viewModel::confirmDelete,
            onDismissDelete = viewModel::dismissDelete,
            onOpenSearch = viewModel::openSearch,
            onCloseSearch = viewModel::closeSearch,
            onSearchQueryChange = viewModel::onSearchQueryChange,
            onBack = { navController.popBackStack() },
        )
    }
}

private fun ChatLauncherViewModel.launchPromptConversation(
    template: PromptTemplate,
    region: PageRegion?,
    mangaId: Long,
    options: ChatLaunchOptions,
) {
    if (region == null) return
    startConversation(
        template = template,
        region = region,
        mangaId = mangaId,
        options = options,
    )
}

private fun resolveTapToNavigate(
    orientation: Int,
    portrait: Boolean,
    landscape: Boolean,
): Boolean = if (orientation == Configuration.ORIENTATION_LANDSCAPE) landscape else portrait
