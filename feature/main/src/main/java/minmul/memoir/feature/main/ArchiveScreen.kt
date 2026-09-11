package minmul.memoir.feature.main

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import minmul.memoir.core.design.ImageThumbnail
import minmul.memoir.core.design.R
import minmul.memoir.core.design.analysisStatusText
import minmul.memoir.core.design.theme.MemoirTheme
import minmul.memoir.core.model.ItemDetail
import minmul.memoir.core.model.JobStatus
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

@Composable
fun ArchiveScreen(
    onOpenItem: (String) -> Unit,
    modifier: Modifier = Modifier,
    items: List<ItemDetail> = emptyList(),
    loading: Boolean = false,
    failed: Boolean = false,
) {
    LazyColumn(modifier.fillMaxSize()) {
        if (loading) item { CircularProgressIndicator() }
        if (failed) item { Text(stringResource(R.string.queue_load_failed)) }
        if (!loading && !failed && items.isEmpty()) item {
            Text(stringResource(R.string.archive_empty), Modifier.padding(16.dp))
        }
        items(items, key = { it.itemId }) { item ->
            ListItem(
                onClick = { onOpenItem(item.itemId) },
                leadingContent = { ImageThumbnail(item.imagePath, Modifier.size(64.dp)) },
                supportingContent = {
                    Column {
                        Text(createdAtLabel(item.createdAt))
                        item.summary?.let {
                            Text(
                                it,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        if (item.status != JobStatus.Succeeded) {
                            Text(analysisStatusText(item.status))
                        }
                    }
                },
            ) {
                Text(item.title ?: stringResource(R.string.archive_untitled))
            }
        }
    }
}

@Composable
private fun createdAtLabel(createdAt: Long): String {
    val formatter = remember {
        DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(Locale.getDefault())
    }
    return remember(createdAt, formatter) {
        formatter.format(
            Instant.ofEpochMilli(createdAt).atZone(ZoneId.systemDefault()).toLocalDate(),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ArchiveScreenPreview() {
    MemoirTheme {
        ArchiveScreen(
            onOpenItem = {},
            items = listOf(
                ItemDetail(
                    itemId = "1",
                    imagePath = "preview",
                    status = JobStatus.Succeeded,
                    ocrText = null,
                    createdAt = 1_725_926_400_000L,
                    title = stringResource(R.string.preview_analysis_title),
                    summary = stringResource(R.string.preview_analysis_summary),
                    detailedSummary = stringResource(R.string.preview_analysis_detailed),
                ),
                ItemDetail(
                    itemId = "2",
                    imagePath = "preview",
                    status = JobStatus.Failed,
                    ocrText = null,
                    createdAt = 1_725_926_400_000L,
                    title = null,
                    summary = null,
                    detailedSummary = null,
                ),
            ),
        )
    }
}
