package minmul.memoir.core.design.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import minmul.memoir.core.design.R
import minmul.memoir.core.design.theme.MemoirTheme
import minmul.memoir.core.model.GemmaModel
import minmul.memoir.core.model.GemmaModelState
import minmul.memoir.core.model.GemmaModelStatus
import minmul.memoir.core.model.OcrModel
import minmul.memoir.core.model.OcrModelState
import minmul.memoir.core.model.OcrModelStatus

@Composable
fun GemmaModelRow(
    modifier: Modifier = Modifier,
    state: GemmaModelState,
    onInstall: () -> Unit,
    onCancel: () -> Unit,
    onSelect: () -> Unit,
    selectionEditable: Boolean,
    onDelete: (() -> Unit)? = null,
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
                            if (!state.selected && onDelete != null) {
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
fun OcrModelRow(
    modifier: Modifier = Modifier,
    state: OcrModelState,
    onInstall: () -> Unit,
    onEnabledChange: ((Boolean) -> Unit)? = null,
    selectionEditable: Boolean = true,
    recommended: Boolean = false,
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
            OcrModelStatus.Ready -> when {
                onEnabledChange == null -> R.string.onboarding_model_already_installed
                state.enabled -> R.string.ocr_model_enabled
                else -> R.string.ocr_model_disabled
            }

            OcrModelStatus.Failed -> R.string.ocr_model_failed
        }
    )
    val progress = state.progress
    val enableLabel = stringResource(R.string.ocr_model_enable_named, name)
    val downloadLabel = stringResource(R.string.ocr_model_download_named, name)
    Column(modifier = modifier) {
        ListItem(
            overlineContent = if (recommended) {
                { Text(stringResource(R.string.onboarding_model_recommended)) }
            } else null,
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
                if (state.status == OcrModelStatus.Ready && onEnabledChange != null) {
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

class GemmaModelRowPreviewParameterProvider : PreviewParameterProvider<GemmaModelState> {
    override val values = sequenceOf(
        GemmaModelState(GemmaModel.E4B, GemmaModelStatus.Ready, selected = true),
        GemmaModelState(GemmaModel.E2B, GemmaModelStatus.Ready),
        GemmaModelState(GemmaModel.E2B, GemmaModelStatus.Missing),
        GemmaModelState(GemmaModel.E2B, GemmaModelStatus.Downloading, 42, 100),
        GemmaModelState(GemmaModel.E4B, GemmaModelStatus.Pending),
        GemmaModelState(GemmaModel.E4B, GemmaModelStatus.Paused, 42, 100),
        GemmaModelState(GemmaModel.E4B, GemmaModelStatus.Checking),
        GemmaModelState(GemmaModel.E2B, GemmaModelStatus.Failed),
    )
}

@Preview(name = "Settings states", showBackground = true, widthDp = 360)
@Composable
private fun GemmaModelRowPreview(
    @PreviewParameter(GemmaModelRowPreviewParameterProvider::class) state: GemmaModelState,
) {
    MemoirTheme {
        GemmaModelRow(
            state = state,
            onInstall = {},
            onCancel = {},
            onSelect = {},
            onDelete = {},
            selectionEditable = true,
        )
    }
}

@Preview(name = "Onboarding without delete", showBackground = true, widthDp = 360)
@Composable
private fun GemmaModelRowOnboardingPreview() {
    MemoirTheme {
        GemmaModelRow(
            state = GemmaModelState(GemmaModel.E2B, GemmaModelStatus.Ready),
            onInstall = {},
            onCancel = {},
            onSelect = {},
            selectionEditable = true,
        )
    }
}

