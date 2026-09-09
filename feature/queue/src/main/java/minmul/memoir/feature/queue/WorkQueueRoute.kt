package minmul.memoir.feature.queue

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import minmul.memoir.core.model.LlmRuntimeStatus

@Composable
fun WorkQueueRoute(
    onOpenHistory: () -> Unit,
    onOpenItem: (String) -> Unit,
    onStart: () -> Unit,
    serviceFailed: Boolean,
    llmStatus: LlmRuntimeStatus,
    modifier: Modifier = Modifier,
    viewModel: WorkQueueViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val actionFailed by viewModel.actionFailed.collectAsStateWithLifecycle()
    WorkQueueScreen(
        items = state.items,
        isLoading = state.isLoading,
        failed = state.failed,
        onOpenHistory = onOpenHistory,
        onCancel = viewModel::cancel,
        onOpenItem = onOpenItem,
        onStart = onStart,
        actionFailed = actionFailed,
        serviceFailed = serviceFailed,
        llmStatus = llmStatus,
        modifier = modifier,
    )
}
