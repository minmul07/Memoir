package minmul.memoir.feature.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun ModelManagementRoute(
    modifier: Modifier = Modifier,
    viewModel: OcrModelViewModel = hiltViewModel(),
) {
    val modelState by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) { viewModel.refresh() }
    ModelManagementScreen(
        modifier = modifier,
        ocrModels = modelState.models,
        preferencesLoaded = modelState.preferencesLoaded,
        preferencesFailed = modelState.preferencesFailed,
        savingModels = modelState.savingModels,
        onOcrEnabledChange = viewModel::setEnabled,
        onInstallOcr = viewModel::install,
        onRefreshOcr = viewModel::refresh,
    )
}
