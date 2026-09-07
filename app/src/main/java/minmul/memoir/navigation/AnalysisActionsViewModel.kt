package minmul.memoir.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import minmul.memoir.data.content.AnalysisRepository

@HiltViewModel
class AnalysisActionsViewModel @Inject constructor(private val repository: AnalysisRepository) : ViewModel() {
    private val mutableBusy = MutableStateFlow(false)
    val busy = mutableBusy.asStateFlow()
    private val mutableFailed = MutableStateFlow(false)
    val failed = mutableFailed.asStateFlow()

    fun cancel(id: String) = perform { repository.cancel(id) }
    fun deleteQueue() = perform { repository.deleteQueue() }
    fun deleteAllItems() = perform { repository.deleteAllItems() }
    fun deleteItem(id: String, onDeleted: () -> Unit) = perform {
        repository.deleteItem(id)
        onDeleted()
    }
    private fun perform(action: suspend () -> Unit) {
        if (mutableBusy.value) return
        mutableBusy.value = true
        mutableFailed.value = false
        viewModelScope.launch {
            try { action() }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { mutableFailed.value = true }
            finally { mutableBusy.value = false }
        }
    }
}
