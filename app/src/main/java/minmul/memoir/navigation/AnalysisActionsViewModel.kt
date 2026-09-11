package minmul.memoir.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import minmul.memoir.core.ai.LlmEngine
import minmul.memoir.data.content.AnalysisRepository
import javax.inject.Inject

@HiltViewModel
class AnalysisActionsViewModel @Inject constructor(
    private val repository: AnalysisRepository,
    llm: LlmEngine,
) : ViewModel() {
    private val mutableBusy = MutableStateFlow(false)
    val busy = mutableBusy.asStateFlow()
    private val mutableFailed = MutableStateFlow(false)
    val failed = mutableFailed.asStateFlow()
    val llmStatus = llm.status

    fun deleteQueue() = perform { repository.deleteQueue() }
    fun deleteAllItems() = perform { repository.deleteAllItems() }

    private fun perform(action: suspend () -> Unit) {
        if (mutableBusy.value) return
        mutableBusy.value = true
        mutableFailed.value = false
        viewModelScope.launch {
            try {
                action()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                mutableFailed.value = true
            } finally {
                mutableBusy.value = false
            }
        }
    }
}
