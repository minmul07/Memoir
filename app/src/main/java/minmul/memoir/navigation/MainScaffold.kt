package minmul.memoir.navigation

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import minmul.memoir.background.analysis.AnalysisService
import minmul.memoir.core.design.R
import minmul.memoir.feature.main.ArchiveRoute
import minmul.memoir.feature.main.HomeScreen
import minmul.memoir.feature.queue.WorkQueueRoute
import minmul.memoir.intake.IntakeIntents

private enum class MainTab(
    @StringRes val labelRes: Int,
) {
    Archive(R.string.nav_archive),
    Home(R.string.nav_dashboard),
    Queue(R.string.nav_queue),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScaffold(
    onOpenSettings: () -> Unit,
    onOpenItem: (String) -> Unit,
    onOpenHistory: () -> Unit,
    modifier: Modifier = Modifier,
    openQueue: Boolean = false,
    onOpenQueueConsumed: () -> Unit = {},
) {
    val actions: AnalysisActionsViewModel = hiltViewModel()
    val serviceFailed by AnalysisService.failed.collectAsStateWithLifecycle()
    val llmStatus by actions.llmStatus.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(),
    ) { uris ->
        if (uris.isEmpty()) {
            return@rememberLauncherForActivityResult
        }
        context.startActivity(IntakeIntents.picker(context, uris))
    }
    var selectedTab by rememberSaveable { mutableStateOf(MainTab.Home) }

    LaunchedEffect(openQueue) {
        if (openQueue) {
            selectedTab = MainTab.Queue
            onOpenQueueConsumed()
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(selectedTab.labelRes)) },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = stringResource(R.string.nav_settings),
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            if (selectedTab == MainTab.Home) {
                FloatingActionButton(
                    onClick = {
                        photoPicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                        )
                    },
                ) {
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
                        onClick = { selectedTab = tab },
                        icon = {
                            when (tab) {
                                MainTab.Archive -> Icon(
                                    painter = painterResource(R.drawable.ic_nav_archive),
                                    contentDescription = stringResource(tab.labelRes),
                                )

                                MainTab.Home -> Icon(
                                    imageVector = Icons.Filled.Home,
                                    contentDescription = stringResource(tab.labelRes),
                                )

                                MainTab.Queue -> Icon(
                                    imageVector = Icons.AutoMirrored.Filled.List,
                                    contentDescription = stringResource(tab.labelRes),
                                )
                            }
                        },
                        label = { Text(stringResource(tab.labelRes)) },
                    )
                }
            }
        },
    ) { innerPadding ->
        val contentModifier = Modifier.padding(innerPadding)
        when (selectedTab) {
            MainTab.Archive -> ArchiveRoute(
                onOpenItem = onOpenItem,
                modifier = contentModifier,
            )

            MainTab.Home -> HomeScreen(
                onOpenItem = onOpenItem,
                modifier = contentModifier,
            )

            MainTab.Queue -> WorkQueueRoute(
                onOpenHistory = onOpenHistory,
                onOpenItem = onOpenItem,
                onStart = { AnalysisService.start(context) },
                serviceFailed = serviceFailed,
                llmStatus = llmStatus,
                modifier = contentModifier,
            )
        }
    }
}
