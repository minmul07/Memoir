package minmul.memoir.feature.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import minmul.memoir.core.design.R
import minmul.memoir.core.design.component.CheckboxItem
import minmul.memoir.core.design.component.ItemSection
import minmul.memoir.core.design.component.NavigationItem
import minmul.memoir.core.design.theme.MemoirTheme
import minmul.memoir.core.model.OcrModel
import minmul.memoir.core.model.OcrModelState
import minmul.memoir.core.model.OcrModelStatus

@Composable
fun ModelManagementScreen(
    modifier: Modifier = Modifier,
    ocrModels: List<OcrModelState> = OcrModel.entries.map { OcrModelState(it) },
    onInstallOcr: (OcrModel) -> Unit = {},
    onRefreshOcr: () -> Unit = {},
    onOcrEnabledChange: (OcrModel, Boolean) -> Unit = { _, _ -> },
    preferencesLoaded: Boolean = true,
    preferencesFailed: Boolean = false,
    savingModels: Set<OcrModel> = emptySet(),
) {
    Column(modifier = modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState())) {
        ItemSection(title = stringResource(R.string.model_section_multimodal)) {
            CheckboxItem(
                title = stringResource(R.string.model_gemma_4_e4b),
                checked = true,
                onCheckedChange = {},
            )
            CheckboxItem(
                title = stringResource(R.string.model_gemma_4_e2b),
                checked = false,
                onCheckedChange = {},
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
            ocrModels = listOf(
                OcrModelState(OcrModel.Korean, OcrModelStatus.Ready),
                OcrModelState(OcrModel.Japanese, OcrModelStatus.Downloading, 42, 100),
                OcrModelState(OcrModel.Chinese, OcrModelStatus.Missing),
                OcrModelState(OcrModel.Latin, OcrModelStatus.Pending),
                OcrModelState(OcrModel.Devanagari, OcrModelStatus.Failed),
            )
        )
    }
}
