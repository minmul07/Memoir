package minmul.memoir.feature.main

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun ItemDetailRoute(
    itemId: String,
    onDeleted: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ItemDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    val actionFailed by viewModel.actionFailed.collectAsStateWithLifecycle()
    LaunchedEffect(itemId) { viewModel.load(itemId) }
    ItemDetailScreen(
        itemId = itemId,
        item = state.item,
        loading = state.loading,
        failed = state.failed || actionFailed,
        busy = busy,
        onDelete = { viewModel.deleteItem(itemId, onDeleted) },
        modifier = modifier,
    )
}
