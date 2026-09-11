package minmul.memoir.feature.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun SettingsRoute(
    onBack: () -> Unit,
    onOpenModelManagement: () -> Unit,
    onOpenDeveloperOptions: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    SettingsScreen(
        onBack = onBack,
        analysisQueueMode = state.analysisQueueMode,
        analysisQueueModeLoaded = state.preferencesLoaded,
        analysisQueueModeFailed = state.preferencesFailed,
        onAnalysisQueueModeChange = viewModel::setAnalysisQueueMode,
        onOpenModelManagement = onOpenModelManagement,
        onOpenDeveloperOptions = onOpenDeveloperOptions,
        modifier = modifier,
    )
}
