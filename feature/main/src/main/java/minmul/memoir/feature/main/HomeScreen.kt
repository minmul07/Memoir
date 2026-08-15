package minmul.memoir.feature.main

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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

const val PlaceholderItemId = "placeholder"

@Composable
fun HomeScreen(
    onOpenArchive: () -> Unit,
    onOpenItem: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
    ) {
        Text(
            text = stringResource(R.string.home_recent_section),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
        )
        ListItem(
            onClick = { onOpenItem(PlaceholderItemId) },
        ) {
            Text(stringResource(R.string.home_recent_item))
        }
        TextButton(
            onClick = onOpenArchive,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.action_more))
        }
        Text(
            text = stringResource(R.string.home_review_section),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
        )
        ListItem {
            Text(stringResource(R.string.home_empty))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    MemoirTheme {
        HomeScreen(
            onOpenArchive = {},
            onOpenItem = {},
        )
    }
}
