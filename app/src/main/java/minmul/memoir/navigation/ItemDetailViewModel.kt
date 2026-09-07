package minmul.memoir.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import minmul.memoir.core.model.ItemDetail
import minmul.memoir.data.content.AnalysisRepository

data class ItemDetailUiState(val item: ItemDetail? = null, val loading: Boolean = true, val failed: Boolean = false)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ItemDetailViewModel @Inject constructor(repository: AnalysisRepository) : ViewModel() {
    private val itemId = MutableStateFlow<String?>(null)
    val uiState = itemId.filterNotNull().flatMapLatest { id ->
        repository.observeDetail(id).map { ItemDetailUiState(item = it, loading = false) }
            .catch { emit(ItemDetailUiState(loading = false, failed = true)) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ItemDetailUiState())
    fun load(id: String) { itemId.value = id }
}
