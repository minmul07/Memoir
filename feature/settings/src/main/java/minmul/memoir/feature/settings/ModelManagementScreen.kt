package minmul.memoir.feature.settings

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSliderState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import minmul.memoir.core.design.R
import minmul.memoir.core.design.component.DialogItem
import minmul.memoir.core.design.component.GemmaModelRow
import minmul.memoir.core.design.component.ItemSection
import minmul.memoir.core.design.component.NavigationItem
import minmul.memoir.core.design.component.OcrModelRow
import minmul.memoir.core.design.component.StackScaffold
import minmul.memoir.core.design.component.ToggleItem
import minmul.memoir.core.design.theme.MemoirTheme
import minmul.memoir.core.model.GemmaInferenceSettings
import minmul.memoir.core.model.GemmaModel
import minmul.memoir.core.model.GemmaModelState
import minmul.memoir.core.model.GemmaModelStatus
import minmul.memoir.core.model.OcrModel
import minmul.memoir.core.model.OcrModelState
import minmul.memoir.core.model.OcrModelStatus
import kotlin.math.roundToInt

@Composable
fun ModelManagementScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    gemmaModels: List<GemmaModelState> = GemmaModel.entries.map { GemmaModelState(it) },
    onInstallGemma: (GemmaModel) -> Unit = {},
    onCancelGemma: (GemmaModel) -> Unit = {},
    onSelectGemma: (GemmaModel) -> Unit = {},
    onDeleteGemma: (GemmaModel) -> Unit = {},
    gemmaPreferencesLoaded: Boolean = true,
    gemmaPreferencesFailed: Boolean = false,
    gemmaSavingModels: Set<GemmaModel> = emptySet(),
    inference: GemmaInferenceSettings = GemmaInferenceSettings(),
    inferenceSaving: Boolean = false,
    onMaxOutputTokenChange: (Int) -> Unit = {},
    onTopKChange: (Int) -> Unit = {},
    onTopPChange: (Double) -> Unit = {},
    onTemperatureChange: (Double) -> Unit = {},
    onThinkingEnabledChange: (Boolean) -> Unit = {},
    onSpeculativeDecodingChange: (Boolean) -> Unit = {},
    ocrModels: List<OcrModelState> = OcrModel.entries.map { OcrModelState(it) },
    onInstallOcr: (OcrModel) -> Unit = {},
    onRefreshOcr: () -> Unit = {},
    onOcrEnabledChange: (OcrModel, Boolean) -> Unit = { _, _ -> },
    preferencesLoaded: Boolean = true,
    preferencesFailed: Boolean = false,
    savingModels: Set<OcrModel> = emptySet(),
) {
    var deleteConfirm: GemmaModel? by remember { mutableStateOf(null) }
    var inferenceDialog by remember { mutableStateOf<InferenceDialog?>(null) }
    val inferenceEditable = gemmaPreferencesLoaded && !inferenceSaving
    StackScaffold(
        title = stringResource(R.string.nav_model_management),
        onBack = onBack,
        modifier = modifier,
    ) { contentModifier ->
        Column(
            modifier = contentModifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            ItemSection(title = stringResource(R.string.model_section_multimodal)) {
                Text(
                    text = stringResource(R.string.gemma_models_policy),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
                if (gemmaPreferencesFailed) {
                    Text(
                        stringResource(R.string.ocr_model_preferences_failed),
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }
                gemmaModels.forEach { state ->
                    key(state.model) {
                        GemmaModelRow(
                            state = state,
                            onInstall = { onInstallGemma(state.model) },
                            onCancel = { onCancelGemma(state.model) },
                            onSelect = { onSelectGemma(state.model) },
                            onDelete = { deleteConfirm = state.model },
                            selectionEditable = gemmaPreferencesLoaded && state.model !in gemmaSavingModels,
                        )
                    }
                }
                DialogItem(
                    title = stringResource(R.string.gemma_max_output_tokens),
                    value = inference.maxOutputToken.toString(),
                    onClick = {
                        if (inferenceEditable) inferenceDialog = InferenceDialog.MaxOutputToken
                    },
                )
                DialogItem(
                    title = stringResource(R.string.gemma_top_k),
                    value = inference.topK.toString(),
                    onClick = { if (inferenceEditable) inferenceDialog = InferenceDialog.TopK },
                )
                DialogItem(
                    title = stringResource(R.string.gemma_top_p),
                    value = inference.topP.toString(),
                    onClick = { if (inferenceEditable) inferenceDialog = InferenceDialog.TopP },
                )
                DialogItem(
                    title = stringResource(R.string.gemma_temperature),
                    value = inference.temperature.toString(),
                    onClick = {
                        if (inferenceEditable) inferenceDialog = InferenceDialog.Temperature
                    },
                )
                ToggleItem(
                    title = stringResource(R.string.gemma_thinking),
                    description = stringResource(R.string.gemma_thinking_description),
                    checked = inference.thinkingEnabled,
                    onCheckedChange = { if (inferenceEditable) onThinkingEnabledChange(it) },
                )
                ToggleItem(
                    title = stringResource(R.string.gemma_speculative_decoding),
                    description = stringResource(R.string.gemma_speculative_decoding_description),
                    checked = inference.speculativeDecodingEnabled,
                    onCheckedChange = { if (inferenceEditable) onSpeculativeDecodingChange(it) },
                )
            }
            ItemSection(title = stringResource(R.string.model_section_ocr)) {
                Text(
                    text = stringResource(R.string.ocr_models_policy),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
                TextButton(
                    onClick = onRefreshOcr,
                    enabled = ocrModels.none { it.status == OcrModelStatus.Checking },
                    modifier = Modifier.padding(horizontal = 8.dp),
                ) { Text(stringResource(R.string.ocr_models_refresh)) }
                if (preferencesFailed) {
                    Text(
                        stringResource(R.string.ocr_model_preferences_failed),
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }
                ocrModels.forEach { state ->
                    key(state.model) {
                        OcrModelRow(
                            state = state,
                            onInstall = { onInstallOcr(state.model) },
                            onEnabledChange = { onOcrEnabledChange(state.model, it) },
                            selectionEditable = preferencesLoaded && state.model !in savingModels,
                        )
                    }
                }
            }
            ItemSection(title = stringResource(R.string.model_section_embedding)) {
                NavigationItem(
                    title = stringResource(R.string.model_embedding_gemma),
                    onClick = {},
                    enabled = false,
                )
            }
        }
    }
    val pendingDelete = deleteConfirm
    if (pendingDelete != null) {
        AlertDialog(
            onDismissRequest = { deleteConfirm = null },
            title = { Text(stringResource(R.string.action_delete)) },
            text = { Text(stringResource(R.string.gemma_model_delete_confirmation)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        deleteConfirm = null
                        onDeleteGemma(pendingDelete)
                    },
                ) { Text(stringResource(R.string.action_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { deleteConfirm = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
    when (inferenceDialog) {
        InferenceDialog.MaxOutputToken -> InferenceSliderDialog(
            title = stringResource(R.string.gemma_max_output_tokens),
            value = inference.maxOutputToken.toFloat(),
            valueRange = GemmaInferenceSettings.MIN_MAX_OUTPUT_TOKEN.toFloat()..
                    GemmaInferenceSettings.MAX_MAX_OUTPUT_TOKEN.toFloat(),
            steps = (GemmaInferenceSettings.MAX_MAX_OUTPUT_TOKEN -
                    GemmaInferenceSettings.MIN_MAX_OUTPUT_TOKEN) /
                    GemmaInferenceSettings.MAX_OUTPUT_TOKEN_STEP - 1,
            format = {
                GemmaInferenceSettings.clamp(maxOutputToken = it.toInt()).maxOutputToken.toString()
            },
            onConfirm = { onMaxOutputTokenChange(it.toInt()) },
            onDismiss = { inferenceDialog = null },
        )

        InferenceDialog.TopK -> InferenceSliderDialog(
            title = stringResource(R.string.gemma_top_k),
            value = inference.topK.toFloat(),
            valueRange = GemmaInferenceSettings.MIN_TOP_K.toFloat()..
                    GemmaInferenceSettings.MAX_TOP_K.toFloat(),
            steps = GemmaInferenceSettings.MAX_TOP_K - GemmaInferenceSettings.MIN_TOP_K - 1,
            format = { GemmaInferenceSettings.clamp(topK = it.toInt()).topK.toString() },
            onConfirm = { onTopKChange(it.toInt()) },
            onDismiss = { inferenceDialog = null },
        )

        InferenceDialog.TopP -> InferenceSliderDialog(
            title = stringResource(R.string.gemma_top_p),
            value = inference.topP.toFloat(),
            valueRange = GemmaInferenceSettings.MIN_TOP_P.toFloat()..
                    GemmaInferenceSettings.MAX_TOP_P.toFloat(),
            steps = sliderSteps(
                GemmaInferenceSettings.MIN_TOP_P,
                GemmaInferenceSettings.MAX_TOP_P,
                GemmaInferenceSettings.TOP_P_STEP,
            ),
            format = { GemmaInferenceSettings.clamp(topP = it.toDouble()).topP.toString() },
            onConfirm = { onTopPChange(it.toDouble()) },
            onDismiss = { inferenceDialog = null },
        )

        InferenceDialog.Temperature -> InferenceSliderDialog(
            title = stringResource(R.string.gemma_temperature),
            value = inference.temperature.toFloat(),
            valueRange = GemmaInferenceSettings.MIN_TEMPERATURE.toFloat()..
                    GemmaInferenceSettings.MAX_TEMPERATURE.toFloat(),
            steps = sliderSteps(
                GemmaInferenceSettings.MIN_TEMPERATURE,
                GemmaInferenceSettings.MAX_TEMPERATURE,
                GemmaInferenceSettings.TEMPERATURE_STEP,
            ),
            format = {
                GemmaInferenceSettings.clamp(temperature = it.toDouble()).temperature.toString()
            },
            onConfirm = { onTemperatureChange(it.toDouble()) },
            onDismiss = { inferenceDialog = null },
        )

        null -> Unit
    }
}

private enum class InferenceDialog { MaxOutputToken, TopK, TopP, Temperature }

private fun sliderSteps(min: Double, max: Double, step: Double): Int =
    ((max - min) / step).roundToInt() - 1

@Composable
private fun InferenceSliderDialog(
    title: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    format: (Float) -> String,
    onConfirm: (Float) -> Unit,
    onDismiss: () -> Unit,
) {
    var draft by remember { mutableFloatStateOf(value) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                Text(format(draft))
                Slider(
                    state = rememberSliderState(
                        value = draft,
                        steps = steps, trackRange = valueRange
                    ),
                    onValueChange = { draft = it },
                    modifier = Modifier,
                    enabled = true,
                    onValueChangeFinished = null,
                    colors = SliderDefaults.colors(),
                    interactionSource = remember { MutableInteractionSource() })
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(draft)
                    onDismiss()
                },
            ) { Text(stringResource(R.string.action_done)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}

@Preview(showBackground = true)
@Composable
private fun ModelManagementScreenPreview() {
    MemoirTheme {
        ModelManagementScreen(
            onBack = {},
            gemmaModels = listOf(
                GemmaModelState(GemmaModel.E4B, GemmaModelStatus.Ready, selected = true),
                GemmaModelState(GemmaModel.E2B, GemmaModelStatus.Downloading, 42, 100),
            ),
            ocrModels = listOf(
                OcrModelState(OcrModel.Korean, OcrModelStatus.Ready),
                OcrModelState(OcrModel.Japanese, OcrModelStatus.Downloading, 42, 100),
                OcrModelState(OcrModel.Chinese, OcrModelStatus.Missing),
                OcrModelState(OcrModel.Latin, OcrModelStatus.Pending),
                OcrModelState(OcrModel.Devanagari, OcrModelStatus.Failed),
            ),
        )
    }
}
