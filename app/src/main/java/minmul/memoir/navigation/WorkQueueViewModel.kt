package minmul.memoir.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import minmul.memoir.core.model.QueueItem
import minmul.memoir.data.content.ContentRepository

data class WorkQueueUiState(
    val items: List<QueueItem> = emptyList(),
    val isLoading: Boolean = true,
    val failed: Boolean = false,
)

@HiltViewModel
class WorkQueueViewModel @Inject constructor(repository: ContentRepository) : ViewModel() {
    val uiState = repository.observeQueue()
        .map { WorkQueueUiState(items = it, isLoading = false) }
        .catch { emit(WorkQueueUiState(isLoading = false, failed = true)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WorkQueueUiState())
}
