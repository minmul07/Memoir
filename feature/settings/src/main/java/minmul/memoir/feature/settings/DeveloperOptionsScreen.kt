package minmul.memoir.feature.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import minmul.memoir.core.design.R
import minmul.memoir.core.design.theme.MemoirTheme

@Composable
fun DeveloperOptionsScreen(
    onResetOnboarding: () -> Unit,
    modifier: Modifier = Modifier,
    onDeleteQueue: () -> Unit = {},
    onDeleteAllItems: () -> Unit = {},
    busy: Boolean = false,
    failed: Boolean = false,
) {
    val preview = LocalInspectionMode.current
    var deletion by remember { mutableStateOf<Int?>(null) }
    Column(modifier.fillMaxSize()) {
        ListItem(onClick = onResetOnboarding) {
            Text(if (preview) "…" else stringResource(R.string.developer_reset_onboarding))
        }
        ListItem(onClick = { if (!busy) deletion = R.string.developer_delete_queue }) {
            Text(if (preview) "…" else stringResource(R.string.developer_delete_queue))
        }
        ListItem(onClick = { if (!busy) deletion = R.string.developer_delete_items }) {
            Text(if (preview) "…" else stringResource(R.string.developer_delete_items))
        }
        if (busy) CircularProgressIndicator()
        if (failed) Text(if (preview) "…" else stringResource(R.string.content_action_failed))
    }
    deletion?.let { title ->
        AlertDialog(
            onDismissRequest = { deletion = null },
            title = { Text(stringResource(title)) },
            text = { Text(stringResource(if (title == R.string.developer_delete_queue)
                R.string.queue_delete_confirmation else R.string.all_items_delete_confirmation)) },
            confirmButton = { TextButton(onClick = {
                deletion = null
                if (title == R.string.developer_delete_queue) onDeleteQueue() else onDeleteAllItems()
            }) { Text(stringResource(R.string.action_delete)) } },
            dismissButton = { TextButton(onClick = { deletion = null }) {
                Text(stringResource(R.string.action_cancel))
            } },
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun DeveloperOptionsScreenPreview() {
    MemoirTheme { DeveloperOptionsScreen(onResetOnboarding = {}) }
}
