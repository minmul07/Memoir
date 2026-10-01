package minmul.memoir.feature.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import minmul.memoir.core.design.R
import minmul.memoir.core.design.theme.MemoirTheme

@Composable
fun OnboardingCrashlyticsRequestScreen(
    onContinue: () -> Unit,
    onDisagree: () -> Unit,
    modifier: Modifier = Modifier,
    isSaving: Boolean = false,
    hasSaveError: Boolean = false,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.Start,
    ) {
        Text(
            text = stringResource(R.string.onboarding_crashlytics_title),
            style = MaterialTheme.typography.headlineMedium,
        )
        Spacer(modifier = Modifier.height(36.dp))
        Text(
            text = stringResource(R.string.onboarding_crashlytics_body),
            style = MaterialTheme.typography.bodyLarge,
        )
        Spacer(modifier = Modifier.height(24.dp))
        if (hasSaveError) {
            Text(
                text = stringResource(R.string.settings_crash_reports_failed),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(
                onClick = onDisagree,
                enabled = !isSaving,
                modifier = Modifier.weight(1f),
            ) {
                Text(stringResource(R.string.action_disagree))
            }
            Button(
                onClick = onContinue,
                enabled = !isSaving,
                modifier = Modifier.weight(1f),
            ) {
                Text(stringResource(R.string.action_agree))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun OnboardingCrashlyticsRequestScreenPreview() {
    MemoirTheme {
        OnboardingCrashlyticsRequestScreen(onContinue = {}, onDisagree = {})
    }
}
