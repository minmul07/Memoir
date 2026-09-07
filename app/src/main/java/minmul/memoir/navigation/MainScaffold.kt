package minmul.memoir.navigation

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ShortNavigationBar
import androidx.compose.material3.ShortNavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import minmul.memoir.background.analysis.AnalysisService
import minmul.memoir.core.design.R
import minmul.memoir.feature.main.HomeScreen
import minmul.memoir.feature.queue.WorkQueueScreen
import minmul.memoir.feature.settings.DeveloperOptionsScreen
import minmul.memoir.feature.settings.ModelManagementScreen
import minmul.memoir.feature.settings.SettingsDestination
import minmul.memoir.feature.settings.SettingsScreen

private enum class MainTab(
    @StringRes val labelRes: Int,
    val icon: ImageVector,
) {
    Home(R.string.nav_dashboard, Icons.Filled.Home),
    Queue(R.string.nav_queue, Icons.AutoMirrored.Filled.List),
    Settings(R.string.nav_settings, Icons.Filled.Settings),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScaffold(
    onOpenArchive: () -> Unit,
    onOpenItem: (String) -> Unit,
    onOpenHistory: () -> Unit,
    onResetOnboarding: () -> Unit,
    modifier: Modifier = Modifier,
    openQueue: Boolean = false,
    onOpenQueueConsumed: () -> Unit = {},
) {
    val actions: AnalysisActionsViewModel = hiltViewModel()
    val busy by actions.busy.collectAsStateWithLifecycle()
    val actionFailed by actions.failed.collectAsStateWithLifecycle()
    val serviceFailed by AnalysisService.failed.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var selectedTab by rememberSaveable { mutableStateOf(MainTab.Home) }
    var settingsDestination by rememberSaveable { mutableStateOf(SettingsDestination.Root) }
    val canNavigateBack = selectedTab == MainTab.Settings &&
        settingsDestination != SettingsDestination.Root

    LaunchedEffect(openQueue) {
        if (openQueue) {
            selectedTab = MainTab.Queue
            settingsDestination = SettingsDestination.Root
            onOpenQueueConsumed()
        }
    }
    val titleRes = when (selectedTab) {
        MainTab.Home -> selectedTab.labelRes
        MainTab.Queue -> selectedTab.labelRes
        MainTab.Settings -> settingsDestination.labelRes
    }

    BackHandler(enabled = canNavigateBack) {
        settingsDestination = SettingsDestination.Root
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            if (canNavigateBack) {
                TopAppBar(
                    title = { Text(stringResource(titleRes)) },
                    navigationIcon = {
                        IconButton(onClick = { settingsDestination = SettingsDestination.Root }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.action_back),
                            )
                        }
                    },
                )
            } else {
                TopAppBar(
                    title = { Text(stringResource(titleRes)) },
                )
            }
        },
        floatingActionButton = {
            if (selectedTab == MainTab.Home) {
                FloatingActionButton(onClick = {}) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = stringResource(R.string.action_add_to_queue),
                    )
                }
            }
        },
        bottomBar = {
            ShortNavigationBar {
                MainTab.entries.forEach { tab ->
                    ShortNavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = {
                            if (tab == MainTab.Settings && selectedTab == MainTab.Settings) {
                                settingsDestination = SettingsDestination.Root
                            }
                            selectedTab = tab
                        },
                        icon = {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = stringResource(tab.labelRes),
                            )
                        },
                        label = { Text(stringResource(tab.labelRes)) },
                    )
                }
            }
        },
    ) { innerPadding ->
        val contentModifier = Modifier.padding(innerPadding)
        when (selectedTab) {
            MainTab.Home -> HomeScreen(
                onOpenArchive = onOpenArchive,
                onOpenItem = onOpenItem,
                modifier = contentModifier,
            )
            MainTab.Queue -> {
                val viewModel: WorkQueueViewModel = hiltViewModel()
                val state by viewModel.uiState.collectAsStateWithLifecycle()
                WorkQueueScreen(
                    items = state.items,
                    onCancel = actions::cancel,
                    onOpenItem = onOpenItem,
                    onStart = { AnalysisService.start(context) },
                    actionFailed = actionFailed,
                    serviceFailed = serviceFailed,
                    isLoading = state.isLoading,
                    failed = state.failed,
                    onOpenHistory = onOpenHistory,
                    modifier = contentModifier,
                )
            }
            MainTab.Settings -> when (settingsDestination) {
                SettingsDestination.Root -> SettingsScreen(
                    onOpenModelManagement = {
                        settingsDestination = SettingsDestination.ModelManagement
                    },
                    onOpenDeveloperOptions = {
                        settingsDestination = SettingsDestination.DeveloperOptions
                    },
                    modifier = contentModifier,
                )
                SettingsDestination.ModelManagement -> {
                    val model: OcrModelViewModel = hiltViewModel()
                    val modelState by model.state.collectAsStateWithLifecycle()
                    LaunchedEffect(model) { model.refresh() }
                    ModelManagementScreen(
                        modifier = contentModifier,
                        ocrModels = modelState.models,
                        preferencesLoaded = modelState.preferencesLoaded,
                        preferencesFailed = modelState.preferencesFailed,
                        savingModels = modelState.savingModels,
                        onOcrEnabledChange = model::setEnabled,
                        onInstallOcr = model::install,
                        onRefreshOcr = model::refresh,
                    )
                }
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
}
