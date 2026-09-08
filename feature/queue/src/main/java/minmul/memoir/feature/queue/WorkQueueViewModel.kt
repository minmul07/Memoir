package minmul.memoir.feature.queue

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import minmul.memoir.core.model.QueueItem
import minmul.memoir.data.content.AnalysisRepository
import minmul.memoir.data.content.ContentRepository
import javax.inject.Inject

data class WorkQueueUiState(
    val items: List<QueueItem> = emptyList(),
    val isLoading: Boolean = true,
    val failed: Boolean = false,
)

@HiltViewModel
class WorkQueueViewModel @Inject constructor(
    content: ContentRepository,
    private val analysis: AnalysisRepository,
) : ViewModel() {
    val uiState = content.observeQueue()
        .map { WorkQueueUiState(items = it, isLoading = false) }
        .catch { emit(WorkQueueUiState(isLoading = false, failed = true)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WorkQueueUiState())

    private val mutableBusy = MutableStateFlow(false)
    private val mutableActionFailed = MutableStateFlow(false)
    val actionFailed = mutableActionFailed.asStateFlow()

    fun cancel(id: String) {
        if (mutableBusy.value) {
            return
        }
        mutableBusy.value = true
        mutableActionFailed.value = false
        viewModelScope.launch {
            try {
                analysis.cancel(id)
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
