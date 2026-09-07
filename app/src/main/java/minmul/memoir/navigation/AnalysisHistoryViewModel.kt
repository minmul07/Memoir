package minmul.memoir.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.*
import minmul.memoir.data.content.AnalysisRepository

@HiltViewModel
class AnalysisHistoryViewModel @Inject constructor(repository: AnalysisRepository) : ViewModel() {
    val uiState = repository.observeHistory()
        .map { WorkQueueUiState(items = it, isLoading = false) }
        .catch { emit(WorkQueueUiState(isLoading = false, failed = true)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WorkQueueUiState())
}
