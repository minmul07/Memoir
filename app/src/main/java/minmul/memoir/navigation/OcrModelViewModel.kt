package minmul.memoir.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import minmul.memoir.core.ai.OcrModelManager
import minmul.memoir.core.model.OcrModel
import minmul.memoir.core.model.OcrModelState
import minmul.memoir.core.model.OcrModelStatus
import minmul.memoir.data.preferences.OcrModelPreferencesStore
import javax.inject.Inject

data class OcrModelManagementState(
    val models: List<OcrModelState> = OcrModel.entries.map { OcrModelState(it) },
    val preferencesLoaded: Boolean = false,
    val preferencesFailed: Boolean = false,
    val savingModels: Set<OcrModel> = emptySet(),
)

private data class OcrSelectionState(
    val disabled: Set<OcrModel> = emptySet(),
    val loaded: Boolean = false,
    val failed: Boolean = false,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class OcrModelViewModel @Inject constructor(
    private val models: OcrModelManager,
    private val preferences: OcrModelPreferencesStore,
) : ViewModel() {
    private val reload = MutableStateFlow(0)
    private val saving = MutableStateFlow<Set<OcrModel>>(emptySet())
    private val saveFailed = MutableStateFlow(false)
    private val selection = reload.flatMapLatest {
        preferences.disabledOcrModels
            .map { OcrSelectionState(disabled = it, loaded = true) }
            .onStart { emit(OcrSelectionState()) }
            .catch { emit(OcrSelectionState(failed = true)) }
    }
    val state =
        combine(models.models, selection, saving, saveFailed) { models, selection, saving, failed ->
            OcrModelManagementState(
                models = models.map { it.copy(enabled = it.model !in selection.disabled) },
                preferencesLoaded = selection.loaded,
                preferencesFailed = selection.failed || failed,
                savingModels = saving,
            )
        }.stateIn(viewModelScope, SharingStarted.Eagerly, OcrModelManagementState())
    private var refreshJob: Job? = null

    fun refresh() {
        if (refreshJob?.isActive == true) return
        reload.update { it + 1 }
        refreshJob = viewModelScope.launch { models.refresh() }
    }

    fun setEnabled(model: OcrModel, enabled: Boolean) {
        if (!state.value.preferencesLoaded || model in saving.value) return
        if (state.value.models.none { it.model == model && it.status == OcrModelStatus.Ready }) return
        saving.update { it + model }
        saveFailed.value = false
        viewModelScope.launch {
            try {
                preferences.setOcrModelEnabled(model, enabled)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                saveFailed.value = true
            } finally {
                saving.update { it - model }
            }
        }
    }

    fun install(model: OcrModel) {
        viewModelScope.launch {
            try {
                models.install(model)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                // The shared manager publishes failure on the individual model row.
            }
        }
    }
}
