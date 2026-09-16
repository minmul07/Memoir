package minmul.memoir.feature.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import minmul.memoir.core.design.R
import minmul.memoir.core.design.component.DialogItem
import minmul.memoir.core.design.component.ItemSection
import minmul.memoir.core.design.component.NavigationItem
import minmul.memoir.core.design.component.StackScaffold
import minmul.memoir.core.design.theme.MemoirTheme
import minmul.memoir.core.model.AnalysisQueueMode

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenModelManagement: () -> Unit,
    onOpenOpenSourceLicenses: () -> Unit,
    onOpenDeveloperOptions: () -> Unit,
    modifier: Modifier = Modifier,
    analysisQueueMode: AnalysisQueueMode = AnalysisQueueMode.Manual,
    analysisQueueModeLoaded: Boolean = true,
    analysisQueueModeFailed: Boolean = false,
    onAnalysisQueueModeChange: (AnalysisQueueMode) -> Unit = {},
) {
    var showAnalysisModeDialog by remember { mutableStateOf(false) }
    StackScaffold(
        title = stringResource(R.string.nav_settings),
        onBack = onBack,
        modifier = modifier,
    ) { contentModifier ->
    Column(
        modifier = contentModifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        ItemSection(title = stringResource(R.string.settings_section_model)) {
            NavigationItem(
                title = stringResource(SettingsDestination.ModelManagement.labelRes),
                onClick = onOpenModelManagement,
            )
        }
        ItemSection(title = stringResource(R.string.settings_section_user)) {
            DialogItem(
                title = stringResource(R.string.settings_language),
                onClick = {},
            )
            DialogItem(
                title = stringResource(R.string.settings_theme),
                onClick = {},
            )
            DialogItem(
                title = stringResource(R.string.settings_notification),
                onClick = {},
            )
        }
        ItemSection(title = stringResource(R.string.settings_section_queue)) {
            if (analysisQueueModeFailed) {
                Text(
                    stringResource(R.string.settings_analysis_mode_failed),
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
            DialogItem(
                title = stringResource(R.string.settings_analysis_mode),
                value = stringResource(analysisQueueMode.labelRes()),
                onClick = { if (analysisQueueModeLoaded) showAnalysisModeDialog = true },
            )
        }
        ItemSection(title = stringResource(R.string.settings_section_data)) {
            NavigationItem(
                title = stringResource(R.string.settings_export),
                onClick = {},
            )
            NavigationItem(
                title = stringResource(R.string.settings_import),
                onClick = {},
            )
        }
        ItemSection(title = stringResource(R.string.settings_section_about)) {
            NavigationItem(
                title = stringResource(R.string.settings_app_info),
                onClick = {},
            )
            NavigationItem(
                title = stringResource(R.string.settings_open_source_licenses),
                onClick = onOpenOpenSourceLicenses,
            )
            NavigationItem(
                title = stringResource(SettingsDestination.DeveloperOptions.labelRes),
                onClick = onOpenDeveloperOptions,
            )
        }
    }
    }
    if (showAnalysisModeDialog) {
        AnalysisQueueModeDialog(
            selected = analysisQueueMode,
            onSelect = { mode ->
                onAnalysisQueueModeChange(mode)
                showAnalysisModeDialog = false
            },
            onDismiss = { showAnalysisModeDialog = false },
        )
    }
}

@Composable
private fun AnalysisQueueModeDialog(
    selected: AnalysisQueueMode,
    onSelect: (AnalysisQueueMode) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_analysis_mode)) },
        text = {
            Column(modifier = Modifier.selectableGroup()) {
                AnalysisQueueMode.entries.forEach { mode ->
                    val label = stringResource(mode.labelRes())
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = mode == selected,
                                onClick = { onSelect(mode) },
                                role = Role.RadioButton,
                            )
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = mode == selected,
                            onClick = null,
                            modifier = Modifier.clearAndSetSemantics {},
                        )
                        Text(
                            text = label,
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}

@StringRes
private fun AnalysisQueueMode.labelRes(): Int = when (this) {
    AnalysisQueueMode.Manual -> R.string.settings_manual_analysis
    AnalysisQueueMode.Scheduled -> R.string.settings_scheduled_analysis
    AnalysisQueueMode.Immediate -> R.string.queue_analyze_now
}

@Preview(showBackground = true)
@Composable
private fun SettingsScreenPreview() {
    MemoirTheme {
        SettingsScreen(
            onBack = {},
            onOpenModelManagement = {},
            onOpenOpenSourceLicenses = {},
            onOpenDeveloperOptions = {},
        )
    }
}
