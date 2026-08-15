package minmul.memoir.feature.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import minmul.memoir.core.design.R
import minmul.memoir.core.design.component.DialogItem
import minmul.memoir.core.design.component.ItemSection
import minmul.memoir.core.design.component.NavigationItem
import minmul.memoir.core.design.theme.MemoirTheme

@Composable
fun SettingsScreen(
    onOpenModelManagement: () -> Unit,
    onOpenDeveloperOptions: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
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
            DialogItem(
                title = stringResource(R.string.queue_analyze_now),
                value = stringResource(R.string.settings_in_use),
                onClick = {},
            )
            NavigationItem(
                title = stringResource(R.string.settings_scheduled_analysis),
                onClick = {},
                enabled = false,
            )
            NavigationItem(
                title = stringResource(R.string.settings_sleep_analysis),
                onClick = {},
                enabled = false,
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
                onClick = {},
            )
            NavigationItem(
                title = stringResource(SettingsDestination.DeveloperOptions.labelRes),
                onClick = onOpenDeveloperOptions,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingsScreenPreview() {
    MemoirTheme {
        SettingsScreen(
            onOpenModelManagement = {},
            onOpenDeveloperOptions = {},
        )
    }
}
