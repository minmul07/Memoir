package minmul.memoir.feature.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import minmul.memoir.core.design.R
import minmul.memoir.core.design.component.CheckboxItem
import minmul.memoir.core.design.component.ItemSection
import minmul.memoir.core.design.component.NavigationItem
import minmul.memoir.core.design.theme.MemoirTheme

@Composable
fun ModelManagementScreen(
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
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
        ItemSection(title = stringResource(R.string.model_section_embedding)) {
            NavigationItem(
                title = stringResource(R.string.model_embedding_gemma),
                onClick = {},
                enabled = false,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ModelManagementScreenPreview() {
    MemoirTheme {
        ModelManagementScreen()
    }
}
