package minmul.memoir.feature.intake

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun IntakeRoute(
    imageUris: List<String>,
    onOpenOnboarding: () -> Unit,
    onOpenQueue: () -> Unit,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: IntakeViewModel = hiltViewModel(),
) {
    LaunchedEffect(viewModel) {
        viewModel.start(imageUris)
    }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(uiState) {
        when (uiState) {
            IntakeUiState.OpenOnboarding -> onOpenOnboarding()
            IntakeUiState.OpenQueue -> onOpenQueue()
            IntakeUiState.Finish -> onFinish()
            IntakeUiState.Loading, is IntakeUiState.Confirm -> Unit
        }
    }
    when (val state = uiState) {
        is IntakeUiState.Confirm -> IntakeConfirmScreen(
            items = state.drafts.map { draft ->
                IntakeConfirmItem(
                    imageUri = draft.imageUri.toUri(),
                    failed = draft.failed,
                )
            },
            isSubmitting = state.isSubmitting,
            onAdd = viewModel::onAdd,
            onCancel = viewModel::onCancel,
            modifier = modifier,
        )

        else -> Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            LoadingIndicator()
        }
    }
}
