package minmul.memoir.feature.queue

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import minmul.memoir.core.design.*
import minmul.memoir.core.design.R
import minmul.memoir.core.design.theme.MemoirTheme
import minmul.memoir.core.model.*

@Composable
fun AnalysisHistoryScreen(
    onOpenItem: (String) -> Unit,
    modifier: Modifier = Modifier,
    items: List<QueueItem> = emptyList(),
    loading: Boolean = false,
    failed: Boolean = false,
) {
    val preview = LocalInspectionMode.current
    var filter by rememberSaveable { mutableStateOf<JobStatus?>(null) }
    val filtered = items.filter { filter == null || it.status == filter }
    Column(modifier.fillMaxSize()) {
        Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(JobStatus.Cancelled, JobStatus.Failed, JobStatus.Succeeded).forEach { status ->
                FilterChip(
                    selected = filter == status,
                    onClick = { filter = if (filter == status) null else status },
                    label = { Text(analysisStatusText(status)) },
                )
            }
        }
        LazyColumn {
            if (loading) item { CircularProgressIndicator() }
            if (failed) item { Text(if (preview) "…" else stringResource(R.string.queue_load_failed)) }
            if (!loading && !failed && filtered.isEmpty()) item {
                Text(if (preview) "…" else stringResource(R.string.history_empty), Modifier.padding(16.dp))
            }
            itemsIndexed(filtered, key = { _, item -> item.jobId }) { index, item ->
                ListItem(
                    onClick = { onOpenItem(item.itemId) },
                    leadingContent = { ImageThumbnail(item.imagePath, Modifier.size(64.dp)) },
                    supportingContent = { Text(analysisStatusText(item.status)) },
                ) {
                    Text(if (preview) "…" else stringResource(R.string.queue_image_number, index + 1))
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AnalysisHistoryScreenPreview() {
    MemoirTheme { AnalysisHistoryScreen(onOpenItem = {}) }
}
