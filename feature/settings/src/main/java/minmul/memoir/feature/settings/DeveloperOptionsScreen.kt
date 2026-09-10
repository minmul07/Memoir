package minmul.memoir.feature.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
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
    var deletion by remember { mutableStateOf<Int?>(null) }
    Column(modifier.fillMaxSize()) {
        ListItem(onClick = onResetOnboarding) {
            Text(stringResource(R.string.developer_reset_onboarding))
        }
        ListItem(onClick = { if (!busy) deletion = R.string.developer_delete_queue }) {
            Text(stringResource(R.string.developer_delete_queue))
        }
        ListItem(onClick = { if (!busy) deletion = R.string.developer_delete_items }) {
            Text(stringResource(R.string.developer_delete_items))
        }
        if (busy) CircularProgressIndicator()
        if (failed) Text(stringResource(R.string.content_action_failed))
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
