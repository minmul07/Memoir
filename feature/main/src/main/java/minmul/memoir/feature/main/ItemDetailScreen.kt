package minmul.memoir.feature.main

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
fun ItemDetailScreen(
    itemId: String,
    modifier: Modifier = Modifier,
    item: ItemDetail? = null,
    loading: Boolean = false,
    failed: Boolean = false,
    busy: Boolean = false,
    onDelete: () -> Unit = {},
) {
    val preview = LocalInspectionMode.current
    var confirmDelete by remember { mutableStateOf(false) }
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        if (loading) CircularProgressIndicator()
        if (failed) Text(if (preview) "…" else stringResource(R.string.content_action_failed))
        if (!loading && item == null) Text(if (preview) "…" else stringResource(R.string.item_missing))
        item?.let {
            ImageThumbnail(it.imagePath, Modifier.fillMaxWidth().height(240.dp))
            Text(if (preview) "…" else itemId, style = MaterialTheme.typography.titleMedium)
            Text(analysisStatusText(it.status))
            SelectionContainer {
                Column {
                    Text(if (preview) "…" else it.ocrText ?: stringResource(R.string.ocr_empty))
                    Spacer(Modifier.height(16.dp))
                    Text(if (preview) "…" else it.payloadJson ?: stringResource(R.string.item_detail_empty_analysis))
                }
            }
            TextButton(onClick = { confirmDelete = true }, enabled = !busy) {
                Text(if (preview) "…" else stringResource(R.string.action_delete))
            }
        }
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.action_delete)) },
            text = { Text(stringResource(R.string.item_delete_confirmation)) },
            confirmButton = { TextButton(onClick = { confirmDelete = false; onDelete() }) {
                Text(stringResource(R.string.action_delete))
            } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) {
                Text(stringResource(R.string.action_cancel))
            } },
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ItemDetailScreenPreview() {
    MemoirTheme { ItemDetailScreen("…", item = ItemDetail("…", "preview", null, "…", "…")) }
}
