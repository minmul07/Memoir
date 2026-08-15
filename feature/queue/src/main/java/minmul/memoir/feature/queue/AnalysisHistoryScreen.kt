package minmul.memoir.feature.queue

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import minmul.memoir.core.design.R
import minmul.memoir.core.design.theme.MemoirTheme

private const val PlaceholderItemId = "placeholder"

@Composable
fun AnalysisHistoryScreen(
    onOpenItem: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            FilterChip(
                selected = false,
                onClick = {},
                label = { Text(stringResource(R.string.history_filter_cancelled)) },
            )
            FilterChip(
                selected = false,
                onClick = {},
                label = { Text(stringResource(R.string.history_filter_failed)) },
                modifier = Modifier.padding(start = 8.dp),
            )
            FilterChip(
                selected = false,
                onClick = {},
                label = { Text(stringResource(R.string.history_filter_completed)) },
                modifier = Modifier.padding(start = 8.dp),
            )
        }
        ListItem(
            onClick = { onOpenItem(PlaceholderItemId) },
            trailingContent = {
                TextButton(onClick = {}) {
                    Text(stringResource(R.string.action_delete))
                }
            },
        ) {
            Text(stringResource(R.string.history_item))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AnalysisHistoryScreenPreview() {
    MemoirTheme {
        AnalysisHistoryScreen(onOpenItem = {})
    }
}
