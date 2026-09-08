package minmul.memoir.feature.settings

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
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import minmul.memoir.core.ai.AnalysisLog
import minmul.memoir.core.model.GemmaModel
import minmul.memoir.core.model.GemmaModelState
import minmul.memoir.core.model.GemmaModelStatus
import minmul.memoir.data.model.GemmaModelStore
import minmul.memoir.data.preferences.GemmaModelPreferencesStore
import javax.inject.Inject

data class GemmaModelManagementState(
    val models: List<GemmaModelState> = GemmaModel.entries.map { GemmaModelState(it) },
    val selected: GemmaModel? = null,
    val preferencesLoaded: Boolean = false,
    val preferencesFailed: Boolean = false,
    val savingModels: Set<GemmaModel> = emptySet(),
)

private data class GemmaSelectionState(
    val selected: GemmaModel? = null,
    val loaded: Boolean = false,
    val failed: Boolean = false,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class GemmaModelViewModel @Inject constructor(
    private val models: GemmaModelStore,
    private val preferences: GemmaModelPreferencesStore,
) : ViewModel() {
    private val reload = MutableStateFlow(0)
    private val saving = MutableStateFlow<Set<GemmaModel>>(emptySet())
    private val saveFailed = MutableStateFlow(false)
    private var autoSelectAttempted = false
    private val selection = reload.flatMapLatest {
        preferences.selectedGemmaModel
            .map { GemmaSelectionState(selected = it, loaded = true) }
            .onStart { emit(GemmaSelectionState()) }
            .catch {
                AnalysisLog.write("models gemma preferences_failed error=${it.javaClass.simpleName}")
                emit(GemmaSelectionState(failed = true))
            }
    }
    val state =
        combine(models.models, selection, saving, saveFailed) { models, selection, saving, failed ->
            val ready = models.filter { it.status == GemmaModelStatus.Ready }.map { it.model }
            val selected = selection.selected?.takeIf { it in ready }
            GemmaModelManagementState(
                models = models.map { it.copy(selected = it.model == selected) },
                selected = selected,
                preferencesLoaded = selection.loaded,
                preferencesFailed = selection.failed || failed,
                savingModels = saving,
            )
        }.stateIn(viewModelScope, SharingStarted.Eagerly, GemmaModelManagementState())
    private var refreshJob: Job? = null

    init {
        combine(models.models, selection) { models, selection -> models to selection }
            .onEach { (models, selection) -> maybeAutoSelect(models, selection) }
            .launchIn(viewModelScope)
    }

    fun refresh() {
        if (refreshJob?.isActive == true) return
        AnalysisLog.write("models gemma action=refresh")
        reload.update { it + 1 }
        refreshJob = viewModelScope.launch { models.refresh() }
    }

    fun select(model: GemmaModel) {
        if (!state.value.preferencesLoaded || model in saving.value) return
        if (state.value.models.none { it.model == model && it.status == GemmaModelStatus.Ready }) {
            return
        }
        AnalysisLog.write("models gemma action=select model=$model")
        saving.update { it + model }
        saveFailed.value = false
        viewModelScope.launch {
            try {
                preferences.setSelectedGemmaModel(model)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                AnalysisLog.write("models gemma action=select_failed model=$model error=${error.javaClass.simpleName}")
                saveFailed.value = true
            } finally {
                saving.update { it - model }
            }
        }
    }

    fun install(model: GemmaModel) {
        AnalysisLog.write("models gemma action=install model=$model")
        viewModelScope.launch {
            try {
                models.install(model)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                // The shared store publishes failure on the individual model row.
            }
        }
    }

    fun cancel(model: GemmaModel) {
        AnalysisLog.write("models gemma action=cancel model=$model")
        viewModelScope.launch {
            try {
                models.cancel(model)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                AnalysisLog.write("models gemma action=cancel_failed model=$model error=${error.javaClass.simpleName}")
            }
        }
    }

    fun delete(model: GemmaModel) {
        if (state.value.models.any { it.model == model && it.selected }) {
            AnalysisLog.write("models gemma action=delete_skipped model=$model reason=selected")
            return
        }
        if (state.value.models.none { it.model == model && it.status == GemmaModelStatus.Ready }) {
            AnalysisLog.write("models gemma action=delete_skipped model=$model reason=not_ready")
            return
        }
        AnalysisLog.write("models gemma action=delete model=$model")
        viewModelScope.launch {
            try {
                models.delete(model)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                AnalysisLog.write("models gemma action=delete_failed model=$model error=${error.javaClass.simpleName}")
            }
        }
    }

    private suspend fun maybeAutoSelect(
        models: List<GemmaModelState>,
        selection: GemmaSelectionState,
    ) {
        if (!selection.loaded || selection.failed || autoSelectAttempted) return
        val stored =
            selection.selected?.let { selected -> models.firstOrNull { it.model == selected } }
        if (stored != null && (stored.status == GemmaModelStatus.Checking || stored.isDownloading)) return
        val ready = models.filter { it.status == GemmaModelStatus.Ready }.map { it.model }
        if (stored?.status == GemmaModelStatus.Ready || ready.isEmpty()) return
        autoSelectAttempted = true
        val selected = ready.first()
        AnalysisLog.write("models gemma action=auto_select model=$selected")
        try {
            preferences.setSelectedGemmaModel(selected)
        } catch (cancelled: CancellationException) {
            autoSelectAttempted = false
            throw cancelled
        } catch (error: Exception) {
            AnalysisLog.write(
                "models gemma action=auto_select_failed model=$selected error=${error.javaClass.simpleName}",
            )
            saveFailed.value = true
        }
    }
}
