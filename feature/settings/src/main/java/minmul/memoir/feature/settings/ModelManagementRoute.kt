package minmul.memoir.feature.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun ModelManagementRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    ocrViewModel: OcrModelViewModel = hiltViewModel(),
    gemmaViewModel: GemmaModelViewModel = hiltViewModel(),
) {
    val ocrState by ocrViewModel.state.collectAsStateWithLifecycle()
    val gemmaState by gemmaViewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(ocrViewModel, gemmaViewModel) {
        ocrViewModel.refresh()
        gemmaViewModel.refresh()
    }
    ModelManagementScreen(
        onBack = onBack,
        modifier = modifier,
        gemmaModels = gemmaState.models,
        onInstallGemma = gemmaViewModel::install,
        onCancelGemma = gemmaViewModel::cancel,
        onSelectGemma = gemmaViewModel::select,
        onDeleteGemma = gemmaViewModel::delete,
        gemmaPreferencesLoaded = gemmaState.preferencesLoaded,
        gemmaPreferencesFailed = gemmaState.preferencesFailed,
        gemmaSavingModels = gemmaState.savingModels,
        inference = gemmaState.inference,
        inferenceSaving = gemmaState.inferenceSaving,
        onMaxOutputTokenChange = gemmaViewModel::setMaxOutputToken,
        onTopKChange = gemmaViewModel::setTopK,
        onTopPChange = gemmaViewModel::setTopP,
        onTemperatureChange = gemmaViewModel::setTemperature,
        onThinkingEnabledChange = gemmaViewModel::setThinkingEnabled,
        onSpeculativeDecodingChange = gemmaViewModel::setSpeculativeDecodingEnabled,
        ocrModels = ocrState.models,
        preferencesLoaded = ocrState.preferencesLoaded,
        preferencesFailed = ocrState.preferencesFailed,
        savingModels = ocrState.savingModels,
        onOcrEnabledChange = ocrViewModel::setEnabled,
        onInstallOcr = ocrViewModel::install,
        onRefreshOcr = ocrViewModel::refresh,
    )
}
