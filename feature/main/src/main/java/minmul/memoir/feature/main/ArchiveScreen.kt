package minmul.memoir.feature.main

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import minmul.memoir.core.design.R
import minmul.memoir.core.design.theme.MemoirTheme

@Composable
fun ArchiveScreen(
    onOpenItem: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        ListItem(
            onClick = { onOpenItem(PlaceholderItemId) },
        ) {
            Text(stringResource(R.string.archive_item))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ArchiveScreenPreview() {
    MemoirTheme {
        ArchiveScreen(onOpenItem = {})
    }
}
