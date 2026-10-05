package minmul.memoir.feature.onboarding

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import minmul.memoir.core.design.R
import minmul.memoir.core.design.theme.MemoirTheme
import minmul.memoir.core.design.theme.MemoirTypography

@Composable
fun OnboardingPermissionRequestScreen(
    onContinue: () -> Unit,
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier,
    buttonsEnabled: Boolean = true,
) {
    var showCollectionInfo by rememberSaveable { mutableStateOf(false) }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.Start,
        ) {
            Text(
                text = stringResource(R.string.onboarding_permission_title),
                style = MemoirTypography.headlineMedium,
            )
            Spacer(modifier = Modifier.height(36.dp))
            Text(
                text = stringResource(R.string.onboarding_permission_body_1),
                style = MemoirTypography.bodyLarge,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.onboarding_permission_body_2),
                style = MemoirTypography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.onboarding_collection_info_action),
                modifier = Modifier.clickable(
                    interactionSource = null,
                    indication = null,
                    role = Role.Button,
                ) { showCollectionInfo = true },
                style = MemoirTypography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                textDecoration = TextDecoration.Underline,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick = onContinue,
                    enabled = buttonsEnabled,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        stringResource(R.string.action_later),
                        style = MemoirTypography.labelLarge,
                    )
                }
                Button(
                    onClick = onRequestPermission,
                    enabled = buttonsEnabled,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        stringResource(R.string.action_request_permission),
                        style = MemoirTypography.labelLarge,
                    )
                }
            }
        }
    }

    if (showCollectionInfo) {
        Dialog(
            onDismissRequest = { showCollectionInfo = false },
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp)
                    .heightIn(min = 160.dp)
                    .clickable(
                        interactionSource = null,
                        indication = null,
                    ) { showCollectionInfo = false },
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
            ) {
                Text(
                    text = stringResource(R.string.onboarding_collection_info_placeholder),
                    modifier = Modifier.padding(24.dp),
                    style = MemoirTypography.bodyMedium,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun OnboardingPermissionRequestScreenPreview() {
    MemoirTheme {
        OnboardingPermissionRequestScreen(onContinue = {}, onRequestPermission = {})
    }
}

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun OnboardingPermissionRequestScreenDarkPreview() {
    MemoirTheme {
        OnboardingPermissionRequestScreen(onContinue = {}, onRequestPermission = {})
    }
}
