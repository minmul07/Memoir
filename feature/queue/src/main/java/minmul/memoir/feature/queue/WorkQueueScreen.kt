package minmul.memoir.feature.queue

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import minmul.memoir.core.design.R
import minmul.memoir.core.design.theme.MemoirTheme

@Composable
fun WorkQueueScreen(
    onOpenHistory: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
    ) {
        Text(
            text = stringResource(R.string.queue_current),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
        )
        ListItem {
            Text(stringResource(R.string.queue_pending_item))
        }
        LinearProgressIndicator(
            progress = { 0f },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
        )
        Button(
            onClick = {},
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.queue_analyze_now))
        }
        TextButton(
            onClick = {},
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.action_cancel_analysis))
        }
        TextButton(
            onClick = onOpenHistory,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.nav_analysis_history))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun WorkQueueScreenPreview() {
    MemoirTheme {
        WorkQueueScreen(onOpenHistory = {})
    }
}
