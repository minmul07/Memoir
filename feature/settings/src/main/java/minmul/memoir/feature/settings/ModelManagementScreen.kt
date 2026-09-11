package minmul.memoir.feature.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import minmul.memoir.core.design.R
import minmul.memoir.core.design.component.DialogItem
import minmul.memoir.core.design.component.ItemSection
import minmul.memoir.core.design.component.NavigationItem
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
                onClick = { if (inferenceEditable) inferenceDialog = InferenceDialog.Temperature },
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
                    value = draft,
                    onValueChange = { draft = it },
                    valueRange = valueRange,
                    steps = steps,
                )
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

@Composable
private fun GemmaModelRow(
    state: GemmaModelState,
    onInstall: () -> Unit,
    onCancel: () -> Unit,
    onSelect: () -> Unit,
    onDelete: () -> Unit,
    selectionEditable: Boolean,
    modifier: Modifier = Modifier,
) {
    val name = stringResource(
        when (state.model) {
            GemmaModel.E4B -> R.string.model_gemma_4_e4b
            GemmaModel.E2B -> R.string.model_gemma_4_e2b
        }
    )
    val details = stringResource(
        when (state.model) {
            GemmaModel.E4B -> R.string.gemma_model_details_e4b
            GemmaModel.E2B -> R.string.gemma_model_details_e2b
        }
    )
    val status = stringResource(
        when (state.status) {
            GemmaModelStatus.Checking -> R.string.ocr_model_checking
            GemmaModelStatus.Missing -> R.string.ocr_model_missing
            GemmaModelStatus.Pending -> R.string.ocr_model_pending
            GemmaModelStatus.Downloading -> R.string.ocr_model_downloading
            GemmaModelStatus.Paused -> R.string.ocr_model_paused
            GemmaModelStatus.Ready ->
                if (state.selected) R.string.gemma_model_in_use else R.string.ocr_model_ready

            GemmaModelStatus.Failed -> R.string.ocr_model_failed
        }
    )
    val progress = state.progress
    val selectLabel = stringResource(R.string.gemma_model_select_named, name)
    val downloadLabel = stringResource(R.string.ocr_model_download_named, name)
    val cancelLabel = stringResource(R.string.gemma_model_cancel_named, name)
    val deleteLabel = stringResource(R.string.gemma_model_delete_named, name)
    Column(modifier = modifier) {
        ListItem(
            supportingContent = {
                Column {
                    Text(details, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        status,
                        color = if (state.status == GemmaModelStatus.Failed)
                            MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (state.isDownloading && progress != null) {
                        Text(stringResource(R.string.ocr_model_progress, (progress * 100).toInt()))
                    }
                }
            },
            trailingContent = {
                when {
                    state.status == GemmaModelStatus.Ready -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (!state.selected) {
                                TextButton(
                                    onClick = onDelete,
                                    modifier = Modifier.semantics {
                                        contentDescription = deleteLabel
                                    },
                                ) { Text(stringResource(R.string.action_delete)) }
                            }
                            RadioButton(
                                selected = state.selected,
                                onClick = onSelect,
                                enabled = selectionEditable,
                                modifier = Modifier.semantics { contentDescription = selectLabel },
                            )
                        }
                    }

                    state.isDownloading -> {
                        TextButton(
                            onClick = onCancel,
                            modifier = Modifier.semantics { contentDescription = cancelLabel },
                        ) { Text(stringResource(R.string.action_cancel)) }
                    }

                    state.status == GemmaModelStatus.Missing || state.status == GemmaModelStatus.Failed -> {
                        TextButton(
                            onClick = onInstall,
                            modifier = Modifier.semantics { contentDescription = downloadLabel },
                        ) { Text(stringResource(R.string.ocr_model_download)) }
                    }
                }
            },
        ) {
            Text(name)
        }
        if (state.isDownloading || state.status == GemmaModelStatus.Checking) {
            val progressModifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            if (progress != null && state.isDownloading) {
                LinearProgressIndicator(progress = { progress }, modifier = progressModifier)
            } else {
                LinearProgressIndicator(modifier = progressModifier)
            }
        }
        HorizontalDivider()
    }
}

@Composable
private fun OcrModelRow(
    state: OcrModelState,
    onInstall: () -> Unit,
    onEnabledChange: (Boolean) -> Unit,
    selectionEditable: Boolean,
    modifier: Modifier = Modifier,
) {
    val name = stringResource(
        when (state.model) {
            OcrModel.Korean -> R.string.ocr_model_korean
            OcrModel.Japanese -> R.string.ocr_model_japanese
            OcrModel.Chinese -> R.string.ocr_model_chinese
            OcrModel.Latin -> R.string.ocr_model_latin
            OcrModel.Devanagari -> R.string.ocr_model_devanagari
        }
    )
    val status = stringResource(
        when (state.status) {
            OcrModelStatus.Checking -> R.string.ocr_model_checking
            OcrModelStatus.Missing -> R.string.ocr_model_missing
            OcrModelStatus.Pending -> R.string.ocr_model_pending
            OcrModelStatus.Downloading -> R.string.ocr_model_downloading
            OcrModelStatus.Paused -> R.string.ocr_model_paused
            OcrModelStatus.Installing -> R.string.ocr_model_installing
            OcrModelStatus.Ready -> if (state.enabled) R.string.ocr_model_enabled else R.string.ocr_model_disabled
            OcrModelStatus.Failed -> R.string.ocr_model_failed
        }
    )
    val progress = state.progress
    val enableLabel = stringResource(R.string.ocr_model_enable_named, name)
    val downloadLabel = stringResource(R.string.ocr_model_download_named, name)
    Column(modifier = modifier) {
        ListItem(
            supportingContent = {
                Column {
                    Text(
                        status, color = if (state.status == OcrModelStatus.Failed)
                            MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (state.isDownloading && progress != null) {
                        Text(stringResource(R.string.ocr_model_progress, (progress * 100).toInt()))
                    }
                }
            },
            trailingContent = {
                if (state.status == OcrModelStatus.Ready) {
                    Checkbox(
                        checked = state.enabled,
                        onCheckedChange = onEnabledChange,
                        enabled = selectionEditable,
                        modifier = Modifier.semantics { contentDescription = enableLabel },
                    )
                }
                if (state.status == OcrModelStatus.Missing || state.status == OcrModelStatus.Failed) {
                    TextButton(
                        onClick = onInstall,
                        modifier = Modifier.semantics { contentDescription = downloadLabel },
                    ) { Text(stringResource(R.string.ocr_model_download)) }
                }
            },
        ) {
            Text(name)
        }
        if (state.isDownloading || state.status == OcrModelStatus.Checking) {
            val progressModifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            if (progress != null && state.isDownloading) {
                LinearProgressIndicator(progress = { progress }, modifier = progressModifier)
            } else {
                LinearProgressIndicator(modifier = progressModifier)
            }
        }
        HorizontalDivider()
    }
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
