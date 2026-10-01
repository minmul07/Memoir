package minmul.memoir.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
        onDisagreeCrashlytics = viewModel::declineCrashlytics,
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
    onDisagreeCrashlytics: suspend () -> Boolean,
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
                var isSavingCrashlytics by remember { mutableStateOf(false) }
                var hasCrashlyticsSaveError by rememberSaveable { mutableStateOf(false) }
                OnboardingRoute(
                    progress = onboardingProgress,
                    onAdvance = { next ->
                        scope.launch { onAdvanceOnboarding(next) }
                    },
                    onDisagreeCrashlytics = {
                        if (!isSavingCrashlytics) {
                            isSavingCrashlytics = true
                            hasCrashlyticsSaveError = false
                            scope.launch {
                                try {
                                    hasCrashlyticsSaveError = !onDisagreeCrashlytics()
                                } finally {
                                    isSavingCrashlytics = false
                                }
                            }
                        }
                    },
                    isSavingCrashlytics = isSavingCrashlytics,
                    hasCrashlyticsSaveError = hasCrashlyticsSaveError,
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
                AnalysisHistoryRoute(
                    onBack = { backStack.removeLastOrNull() },
                    onOpenItem = { itemId -> backStack.add(ItemDetail(itemId)) },
                )
            }
            entry<ItemDetail> { key ->
                ItemDetailRoute(
                    itemId = key.itemId,
                    onBack = { backStack.removeLastOrNull() },
                    onDeleted = { backStack.removeLastOrNull() },
                )
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

    when (destination) {
        SettingsDestination.Root -> SettingsRoute(
            onBack = onBack,
            onOpenModelManagement = {
                destination = SettingsDestination.ModelManagement
            },
            onOpenDeveloperOptions = {
                destination = SettingsDestination.DeveloperOptions
            },
        )

        SettingsDestination.ModelManagement -> ModelManagementRoute(
            onBack = { destination = SettingsDestination.Root },
        )

        SettingsDestination.DeveloperOptions -> DeveloperOptionsScreen(
            onBack = { destination = SettingsDestination.Root },
            onResetOnboarding = onResetOnboarding,
            onDeleteQueue = actions::deleteQueue,
            onDeleteAllItems = actions::deleteAllItems,
            onTestCrash = { throw IllegalStateException("Memoir test crash") },
            busy = busy,
            failed = actionFailed,
        )
    }
}

private fun NavBackStack<NavKey>.replaceRoot(key: NavKey) {
    while (size > 1) {
        removeLastOrNull()
    }
    this[0] = key
}
