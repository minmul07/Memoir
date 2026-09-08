package minmul.memoir.feature.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import minmul.memoir.core.model.ItemDetail
import minmul.memoir.data.content.AnalysisRepository
import javax.inject.Inject

data class ItemDetailUiState(
    val item: ItemDetail? = null,
    val loading: Boolean = true,
    val failed: Boolean = false,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ItemDetailViewModel @Inject constructor(
    private val repository: AnalysisRepository,
) : ViewModel() {
    private val itemId = MutableStateFlow<String?>(null)
    val uiState = itemId.filterNotNull().flatMapLatest { id ->
        repository.observeDetail(id)
            .map { ItemDetailUiState(item = it, loading = false) }
            .catch { emit(ItemDetailUiState(loading = false, failed = true)) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ItemDetailUiState())

    private val mutableBusy = MutableStateFlow(false)
    val busy = mutableBusy.asStateFlow()
    private val mutableActionFailed = MutableStateFlow(false)
    val actionFailed = mutableActionFailed.asStateFlow()

    fun load(id: String) {
        itemId.value = id
    }

    fun deleteItem(id: String, onDeleted: () -> Unit) {
        if (mutableBusy.value) {
            return
        }
        mutableBusy.value = true
        mutableActionFailed.value = false
        viewModelScope.launch {
            try {
                repository.deleteItem(id)
                onDeleted()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                mutableActionFailed.value = true
            } finally {
                mutableBusy.value = false
            }
        }
    }
}
