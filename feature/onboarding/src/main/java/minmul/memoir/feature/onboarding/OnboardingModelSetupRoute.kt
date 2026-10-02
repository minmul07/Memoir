package minmul.memoir.feature.onboarding

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLocale
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import minmul.memoir.core.ai.defaultOcrModel
import minmul.memoir.core.model.OcrModelState

@Composable
fun OnboardingModelSetupRoute(
    onComplete: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: OnboardingModelSetupViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val locales = LocalConfiguration.current.locales
    val locale = if (locales.isEmpty) LocalLocale.current.platformLocale else locales[0]
    val recommended = defaultOcrModel(locale)
    LaunchedEffect(viewModel) {
        viewModel.refresh()
    }
    OnboardingModelSetupPage(
        onComplete = onComplete,
        modifier = modifier,
        gemmaModels = state.gemmaModels,
        recommendedOcrModel = state.ocrModels.firstOrNull { it.model == recommended }
            ?: OcrModelState(recommended),
        onInstallGemma = viewModel::installGemma,
        onCancelGemma = viewModel::cancelGemma,
        onSelectGemma = viewModel::selectGemma,
        onInstallOcr = viewModel::installOcr,
        onRefresh = viewModel::refresh,
        preferencesLoaded = state.preferencesLoaded,
        preferencesFailed = state.preferencesFailed,
        savingModels = state.savingModels,
        refreshFailed = state.refreshFailed,
    )
}
