package minmul.memoir.feature.main

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import minmul.memoir.core.design.*
import minmul.memoir.core.design.R
import minmul.memoir.core.design.theme.MemoirTheme
import minmul.memoir.core.model.ItemDetail

@Composable
fun ArchiveScreen(
    onOpenItem: (String) -> Unit,
    modifier: Modifier = Modifier,
    items: List<ItemDetail> = emptyList(),
    loading: Boolean = false,
    failed: Boolean = false,
) {
    val preview = LocalInspectionMode.current
    LazyColumn(modifier.fillMaxSize()) {
        if (loading) item { CircularProgressIndicator() }
        if (failed) item { Text(if (preview) "…" else stringResource(R.string.queue_load_failed)) }
        if (!loading && !failed && items.isEmpty()) item {
            Text(if (preview) "…" else stringResource(R.string.archive_empty), Modifier.padding(16.dp))
        }
        itemsIndexed(items, key = { _, item -> item.itemId }) { index, item ->
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

@Preview(showBackground = true)
@Composable
private fun ArchiveScreenPreview() {
    MemoirTheme { ArchiveScreen(onOpenItem = {}) }
}
