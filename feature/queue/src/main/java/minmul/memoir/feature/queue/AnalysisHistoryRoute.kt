package minmul.memoir.feature.queue

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun AnalysisHistoryRoute(
    onOpenItem: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AnalysisHistoryViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    AnalysisHistoryScreen(
        onOpenItem = onOpenItem,
        onBack = onBack,
        items = state.items,
        loading = state.isLoading,
        failed = state.failed,
        modifier = modifier,
    )
}
