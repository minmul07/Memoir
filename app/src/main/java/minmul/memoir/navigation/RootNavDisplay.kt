package minmul.memoir.navigation

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import kotlinx.coroutines.launch
import minmul.memoir.core.design.R
import minmul.memoir.data.preferences.OnboardingProgress
import minmul.memoir.feature.main.ItemDetailRoute
import minmul.memoir.feature.onboarding.OnboardingRoute
import minmul.memoir.feature.queue.AnalysisHistoryRoute
import minmul.memoir.feature.settings.DeveloperOptionsScreen
import minmul.memoir.feature.settings.ModelManagementRoute
import minmul.memoir.feature.settings.SettingsDestination
import minmul.memoir.feature.settings.SettingsRoute

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun RootNavDisplay(
    modifier: Modifier = Modifier,
    openQueue: Boolean = false,
    onOpenQueueConsumed: () -> Unit = {},
    viewModel: RootViewModel = hiltViewModel(),
) {
    val onboardingProgress by viewModel.onboardingProgress.collectAsStateWithLifecycle()
    val progress = onboardingProgress
    if (progress == null) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            LoadingIndicator()
        }
        return
    }

    RootNavDisplay(
        startKey = if (OnboardingProgress.isComplete(progress)) Main else Onboarding,
        onboardingProgress = progress,
        openQueue = openQueue,
        onOpenQueueConsumed = onOpenQueueConsumed,
        onAdvanceOnboarding = viewModel::setOnboardingProgress,
        onCompleteOnboarding = viewModel::completeOnboarding,
        onResetOnboarding = viewModel::resetOnboarding,
        modifier = modifier,
    )
}

@Composable
private fun RootNavDisplay(
    startKey: NavKey,
    onboardingProgress: Int,
    openQueue: Boolean,
    onOpenQueueConsumed: () -> Unit,
    onAdvanceOnboarding: suspend (Int) -> Unit,
    onCompleteOnboarding: suspend () -> Unit,
    onResetOnboarding: suspend () -> Unit,
    modifier: Modifier = Modifier,
) {
    val backStack = rememberNavBackStack(startKey)
    val scope = rememberCoroutineScope()

    LaunchedEffect(openQueue) {
        if (!openQueue) {
            return@LaunchedEffect
        }
        val mainIndex = backStack.indexOfFirst { it is Main }
        if (mainIndex >= 0) {
            while (backStack.size > mainIndex + 1) {
                backStack.removeLastOrNull()
            }
        }
    }

    NavDisplay(
        backStack = backStack,
        modifier = modifier.fillMaxSize(),
        onBack = {
            if (backStack.size > 1) {
                backStack.removeLastOrNull()
            }
        },
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
        ),
        entryProvider = entryProvider {
            entry<Onboarding> {
                OnboardingRoute(
                    progress = onboardingProgress,
                    onAdvance = { next ->
                        scope.launch { onAdvanceOnboarding(next) }
                    },
                    onComplete = {
                        scope.launch {
                            onCompleteOnboarding()
                            backStack.replaceRoot(Main)
                        }
                    },
                )
            }
            entry<Main> {
                MainScaffold(
                    onOpenSettings = { backStack.add(Settings) },
                    onOpenItem = { itemId -> backStack.add(ItemDetail(itemId)) },
                    onOpenHistory = { backStack.add(AnalysisHistory) },
                    openQueue = openQueue,
                    onOpenQueueConsumed = onOpenQueueConsumed,
                )
            }
            entry<Settings> {
                SettingsStack(
                    onBack = { backStack.removeLastOrNull() },
                    onResetOnboarding = {
                        scope.launch {
                            onResetOnboarding()
                            backStack.replaceRoot(Onboarding)
                        }
                    },
                )
            }
            entry<AnalysisHistory> {
                StackScaffold(
                    titleRes = R.string.nav_analysis_history,
                    onBack = { backStack.removeLastOrNull() },
                ) { contentModifier ->
                    AnalysisHistoryRoute(
                        onOpenItem = { itemId -> backStack.add(ItemDetail(itemId)) },
                        modifier = contentModifier,
                    )
                }
            }
            entry<ItemDetail> { key ->
                StackScaffold(
                    titleRes = R.string.nav_item_detail,
                    onBack = { backStack.removeLastOrNull() },
                ) { contentModifier ->
                    ItemDetailRoute(
                        itemId = key.itemId,
                        onDeleted = { backStack.removeLastOrNull() },
                        modifier = contentModifier,
                    )
                }
            }
        },
    )
}

@Composable
private fun SettingsStack(
    onBack: () -> Unit,
    onResetOnboarding: () -> Unit,
) {
    val actions: AnalysisActionsViewModel = hiltViewModel()
    val busy by actions.busy.collectAsStateWithLifecycle()
    val actionFailed by actions.failed.collectAsStateWithLifecycle()
    var destination by rememberSaveable { mutableStateOf(SettingsDestination.Root) }
    val canNavigateBack = destination != SettingsDestination.Root

    BackHandler(enabled = canNavigateBack) {
        destination = SettingsDestination.Root
    }

    StackScaffold(
        titleRes = destination.labelRes,
        onBack = {
            if (canNavigateBack) {
                destination = SettingsDestination.Root
            } else {
                onBack()
            }
        },
    ) { contentModifier ->
        when (destination) {
            SettingsDestination.Root -> SettingsRoute(
                onOpenModelManagement = {
                    destination = SettingsDestination.ModelManagement
                },
                onOpenDeveloperOptions = {
                    destination = SettingsDestination.DeveloperOptions
                },
                modifier = contentModifier,
            )

            SettingsDestination.ModelManagement -> ModelManagementRoute(
                modifier = contentModifier,
            )

            SettingsDestination.DeveloperOptions -> DeveloperOptionsScreen(
                onResetOnboarding = onResetOnboarding,
                onDeleteQueue = actions::deleteQueue,
                onDeleteAllItems = actions::deleteAllItems,
                busy = busy,
                failed = actionFailed,
                modifier = contentModifier,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StackScaffold(
    @StringRes titleRes: Int,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (Modifier) -> Unit,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(titleRes)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        content(Modifier.padding(innerPadding))
    }
}

private fun NavBackStack<NavKey>.replaceRoot(key: NavKey) {
    while (size > 1) {
        removeLastOrNull()
    }
    this[0] = key
}
