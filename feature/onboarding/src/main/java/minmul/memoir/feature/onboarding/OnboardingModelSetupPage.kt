package minmul.memoir.feature.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import minmul.memoir.core.design.R
import minmul.memoir.core.design.component.CheckboxItem
import minmul.memoir.core.design.component.ItemSection
import minmul.memoir.core.design.theme.MemoirTheme

@Composable
fun OnboardingModelSetupPage(
    onComplete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.onboarding_model_setup_title),
            style = MaterialTheme.typography.headlineMedium,
        )
        Spacer(modifier = Modifier.height(24.dp))
        ItemSection(title = stringResource(R.string.model_section_multimodal)) {
            CheckboxItem(
                title = stringResource(R.string.model_gemma_4_e4b),
                checked = true,
                onCheckedChange = {},
            )
            CheckboxItem(
                title = stringResource(R.string.model_gemma_4_e2b),
                checked = false,
                onCheckedChange = {},
            )
        }
        ItemSection(title = stringResource(R.string.model_section_ocr)) {
            CheckboxItem(
                title = stringResource(R.string.model_mlkit_text_recognition_v2),
                checked = true,
                onCheckedChange = {},
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onComplete) {
            Text(stringResource(R.string.action_done))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun OnboardingModelSetupPagePreview() {
    MemoirTheme {
        OnboardingModelSetupPage(onComplete = {})
    }
}
