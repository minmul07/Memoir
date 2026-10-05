package minmul.memoir.feature.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import minmul.memoir.core.design.R
import minmul.memoir.core.design.component.GemmaModelRow
import minmul.memoir.core.design.component.ItemSection
import minmul.memoir.core.design.component.OcrModelRow
import minmul.memoir.core.design.theme.MemoirTheme
import minmul.memoir.core.design.theme.MemoirTypography
import minmul.memoir.core.model.GemmaModel
import minmul.memoir.core.model.GemmaModelState
import minmul.memoir.core.model.GemmaModelStatus
import minmul.memoir.core.model.OcrModel
import minmul.memoir.core.model.OcrModelState
import minmul.memoir.core.model.OcrModelStatus

@Composable
fun OnboardingModelSetupPage(
    onComplete: () -> Unit,
    gemmaModels: List<GemmaModelState>,
    recommendedOcrModel: OcrModelState,
    onInstallGemma: (GemmaModel) -> Unit,
    onCancelGemma: (GemmaModel) -> Unit,
    onSelectGemma: (GemmaModel) -> Unit,
    onInstallOcr: (OcrModel) -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    preferencesLoaded: Boolean = false,
    preferencesFailed: Boolean = false,
    savingModels: Set<GemmaModel> = emptySet(),
    refreshFailed: Boolean = false,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.Start,
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.Start,
        ) {
            Text(
                text = stringResource(R.string.onboarding_model_setup_title),
                style = MemoirTypography.headlineMedium,
            )
            Spacer(modifier = Modifier.height(36.dp))
            Text(
                text = stringResource(R.string.onboarding_model_setup_body),
                style = MemoirTypography.bodyLarge,
            )
            Spacer(modifier = Modifier.height(8.dp))
            if (preferencesFailed || refreshFailed) {
                Text(
                    text = stringResource(
                        if (preferencesFailed) R.string.ocr_model_preferences_failed
                        else R.string.onboarding_model_refresh_failed,
                    ),
                    color = MaterialTheme.colorScheme.error,
                    style = MemoirTypography.bodyMedium,
                )
            }
            ItemSection(title = stringResource(R.string.model_section_multimodal)) {
                Column(modifier = Modifier.selectableGroup()) {
                    gemmaModels.forEach { state ->
                        key(state.model) {
                            GemmaModelRow(
                                state = state,
                                onInstall = { onInstallGemma(state.model) },
                                onCancel = { onCancelGemma(state.model) },
                                onSelect = { onSelectGemma(state.model) },
                                selectionEditable = preferencesLoaded && savingModels.isEmpty(),
                            )
                        }
                    }
                }
            }
            ItemSection(title = stringResource(R.string.model_section_ocr)) {
                OcrModelRow(
                    state = recommendedOcrModel,
                    onInstall = { onInstallOcr(recommendedOcrModel.model) },
                    recommended = true,
                )
            }
            TextButton(onClick = onRefresh) {
                Text(
                    stringResource(R.string.ocr_models_refresh),
                    style = MemoirTypography.labelLarge,
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onComplete, modifier = Modifier.fillMaxWidth()) {
            Text(
                stringResource(R.string.action_done),
                style = MemoirTypography.labelLarge,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun OnboardingModelSetupPagePreview() {
    MemoirTheme {
        OnboardingModelSetupPage(
            onComplete = {},
            gemmaModels = listOf(
                GemmaModelState(GemmaModel.E4B, GemmaModelStatus.Missing),
                GemmaModelState(GemmaModel.E2B, GemmaModelStatus.Downloading, 42, 100),
            ),
            recommendedOcrModel = OcrModelState(OcrModel.Korean, OcrModelStatus.Ready),
            onInstallGemma = {},
            onCancelGemma = {},
            onSelectGemma = {},
            onInstallOcr = {},
            onRefresh = {},
            preferencesLoaded = true,
        )
    }
}
