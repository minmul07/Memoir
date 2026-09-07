package minmul.memoir.feature.queue

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import minmul.memoir.core.design.ImageThumbnail
import minmul.memoir.core.design.R
import minmul.memoir.core.design.analysisStatusText
import minmul.memoir.core.design.theme.MemoirTheme
import minmul.memoir.core.model.JobStatus
import minmul.memoir.core.model.QueueItem

@Composable
fun WorkQueueScreen(
    items: List<QueueItem>,
    isLoading: Boolean,
    failed: Boolean,
    onOpenHistory: () -> Unit,
    modifier: Modifier = Modifier,
    onCancel: (String) -> Unit = {},
    onOpenItem: (String) -> Unit = {},
    onStart: () -> Unit = {},
    actionFailed: Boolean = false,
    serviceFailed: Boolean = false,
) {
    val preview = LocalInspectionMode.current
    Column(modifier = modifier
        .fillMaxSize()
        .padding(horizontal = 16.dp)) {
        Text(
            text = if (preview) "…" else stringResource(R.string.queue_item_count, items.size),
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(vertical = 16.dp),
        )
        LazyColumn(modifier = Modifier
            .weight(1f)
            .fillMaxWidth()) {
            when {
                isLoading -> item { CircularProgressIndicator() }
                failed -> item { Text(if (preview) "…" else stringResource(R.string.queue_load_failed)) }
                items.isEmpty() -> item { Text(if (preview) "…" else stringResource(R.string.queue_empty)) }
            }
            itemsIndexed(items, key = { _, item -> item.jobId }) { index, item ->
                ListItem(
                    onClick = { onOpenItem(item.itemId) },
                    trailingContent = {
                        TextButton(onClick = { onCancel(item.jobId) }) {
                            Text(if (preview) "…" else stringResource(R.string.action_cancel))
                        }
                    },
                    content = {
                        Text(if (preview) "…" else stringResource(R.string.queue_image_number, index + 1))
                    },
                    supportingContent = {
                        Text(analysisStatusText(item.status, item.stage, item.attemptCount))
                    },
                    leadingContent = {
                        ImageThumbnail(imagePath = item.imagePath, modifier = Modifier.size(64.dp))
                    },
                )
            }
        }
        if (actionFailed) Text(stringResource(R.string.content_action_failed))
        if (serviceFailed) Text(stringResource(R.string.analysis_service_failed))
        if (items.isNotEmpty()) TextButton(onClick = onStart) {
            Text(if (preview) "…" else stringResource(R.string.analysis_restart))
        }
        TextButton(onClick = onOpenHistory, modifier = Modifier.fillMaxWidth()) {
            Text(if (preview) "…" else stringResource(R.string.nav_analysis_history))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun WorkQueueScreenPreview() {
    MemoirTheme {
        WorkQueueScreen(
            items = listOf(QueueItem("1", "1", "preview", JobStatus.Queued)),
            isLoading = false,
            failed = false,
            onOpenHistory = {},
        )
    }
}
