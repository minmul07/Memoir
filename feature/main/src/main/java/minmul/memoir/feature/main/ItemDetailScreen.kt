package minmul.memoir.feature.main

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import minmul.memoir.core.design.ImageThumbnail
import minmul.memoir.core.design.R
import minmul.memoir.core.design.analysisStatusText
import minmul.memoir.core.design.theme.MemoirTheme
import minmul.memoir.core.model.ItemDetail
import minmul.memoir.core.model.JobStatus

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
    var confirmDelete by remember { mutableStateOf(false) }
    Column(modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(16.dp)) {
        if (loading) CircularProgressIndicator()
        if (failed) Text(stringResource(R.string.content_action_failed))
        if (!loading && item == null) Text(stringResource(R.string.item_missing))
        item?.let {
            ImageThumbnail(it.imagePath, Modifier
                .fillMaxWidth()
                .height(240.dp))
            Text(itemId, style = MaterialTheme.typography.titleMedium)
            Text(analysisStatusText(it.status))
            SelectionContainer {
                Column {
                    Text(it.ocrText ?: stringResource(R.string.ocr_empty))
                    Spacer(Modifier.height(16.dp))
                    Text(it.payloadJson ?: stringResource(R.string.item_detail_empty_analysis))
                }
            }
            TextButton(onClick = { confirmDelete = true }, enabled = !busy) {
                Text(stringResource(R.string.action_delete))
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
    val itemId = stringResource(R.string.queue_image_number, 1)
    MemoirTheme {
        ItemDetailScreen(
            itemId = itemId,
            item = ItemDetail(
                itemId = itemId,
                imagePath = "preview",
                status = JobStatus.Succeeded,
                ocrText = stringResource(R.string.preview_ocr_text),
                payloadJson = stringResource(R.string.preview_analysis_payload),
            ),
        )
    }
}
