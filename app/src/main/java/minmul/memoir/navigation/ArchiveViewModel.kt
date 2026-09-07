package minmul.memoir.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.*
import minmul.memoir.core.model.ItemDetail
import minmul.memoir.data.content.AnalysisRepository

data class ArchiveUiState(val items: List<ItemDetail> = emptyList(), val loading: Boolean = true, val failed: Boolean = false)

@HiltViewModel
class ArchiveViewModel @Inject constructor(repository: AnalysisRepository) : ViewModel() {
    val uiState = repository.observeItems()
        .map { ArchiveUiState(items = it, loading = false) }
        .catch { emit(ArchiveUiState(loading = false, failed = true)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ArchiveUiState())
}
