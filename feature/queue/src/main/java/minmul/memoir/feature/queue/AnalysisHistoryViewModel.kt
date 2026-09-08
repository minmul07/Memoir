package minmul.memoir.feature.queue

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import minmul.memoir.data.content.AnalysisRepository
import javax.inject.Inject

@HiltViewModel
class AnalysisHistoryViewModel @Inject constructor(repository: AnalysisRepository) : ViewModel() {
    val uiState = repository.observeHistory()
        .map { WorkQueueUiState(items = it, isLoading = false) }
        .catch { emit(WorkQueueUiState(isLoading = false, failed = true)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WorkQueueUiState())
}
