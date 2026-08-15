package minmul.memoir.feature.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import minmul.memoir.core.design.R
import minmul.memoir.core.design.theme.MemoirTheme

@Composable
fun DeveloperOptionsScreen(
    onResetOnboarding: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        ListItem(
            onClick = onResetOnboarding,
        ) {
            Text(stringResource(R.string.developer_reset_onboarding))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DeveloperOptionsScreenPreview() {
    MemoirTheme {
        DeveloperOptionsScreen(onResetOnboarding = {})
    }
}
