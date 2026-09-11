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
import minmul.memoir.core.model.GemmaInferenceSettings
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
    val inference: GemmaInferenceSettings = GemmaInferenceSettings(),
    val inferenceSaving: Boolean = false,
)

private data class GemmaSelectionState(
    val selected: GemmaModel? = null,
    val loaded: Boolean = false,
    val failed: Boolean = false,
)

private data class GemmaInferenceState(
    val settings: GemmaInferenceSettings = GemmaInferenceSettings(),
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
    private val inferenceSaving = MutableStateFlow(false)
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
    private val inference = reload.flatMapLatest {
        preferences.inferenceSettings
            .map { GemmaInferenceState(settings = it, loaded = true) }
            .onStart { emit(GemmaInferenceState()) }
            .catch {
                AnalysisLog.write("models gemma inference preferences_failed error=${it.javaClass.simpleName}")
                emit(GemmaInferenceState(failed = true))
            }
    }
    val state =
        combine(
            models.models,
            selection,
            saving,
            saveFailed,
            combine(inference, inferenceSaving) { inference, saving -> inference to saving },
        ) { models, selection, saving, failed, inference ->
            val ready = models.filter { it.status == GemmaModelStatus.Ready }.map { it.model }
            val selected = selection.selected?.takeIf { it in ready }
            val (inferenceState, inferenceSaving) = inference
            GemmaModelManagementState(
                models = models.map { it.copy(selected = it.model == selected) },
                selected = selected,
                preferencesLoaded = selection.loaded && inferenceState.loaded,
                preferencesFailed = selection.failed || failed || inferenceState.failed,
                savingModels = saving,
                inference = inferenceState.settings,
                inferenceSaving = inferenceSaving,
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

    fun setMaxOutputToken(value: Int) {
        val clamped = GemmaInferenceSettings.clamp(maxOutputToken = value).maxOutputToken
        setInference(
            "set_max_output_token value=$clamped",
            state.value.inference.maxOutputToken == clamped
        ) {
            preferences.setMaxOutputToken(value)
        }
    }

    fun setTopK(value: Int) {
        val clamped = GemmaInferenceSettings.clamp(topK = value).topK
        setInference("set_top_k value=$clamped", state.value.inference.topK == clamped) {
            preferences.setTopK(value)
        }
    }

    fun setTopP(value: Double) {
        val clamped = GemmaInferenceSettings.clamp(topP = value).topP
        setInference("set_top_p value=$clamped", state.value.inference.topP == clamped) {
            preferences.setTopP(value)
        }
    }

    fun setTemperature(value: Double) {
        val clamped = GemmaInferenceSettings.clamp(temperature = value).temperature
        setInference(
            "set_temperature value=$clamped",
            state.value.inference.temperature == clamped
        ) {
            preferences.setTemperature(value)
        }
    }

    fun setThinkingEnabled(enabled: Boolean) {
        setInference(
            "set_thinking enabled=$enabled",
            state.value.inference.thinkingEnabled == enabled
        ) {
            preferences.setThinkingEnabled(enabled)
        }
    }

    fun setSpeculativeDecodingEnabled(enabled: Boolean) {
        setInference(
            "set_speculative_decoding enabled=$enabled",
            state.value.inference.speculativeDecodingEnabled == enabled,
        ) {
            preferences.setSpeculativeDecodingEnabled(enabled)
        }
    }

    private fun setInference(action: String, skip: Boolean, write: suspend () -> Unit) {
        if (!state.value.preferencesLoaded || inferenceSaving.value || skip) return
        AnalysisLog.write("models gemma action=$action")
        inferenceSaving.value = true
        saveFailed.value = false
        viewModelScope.launch {
            try {
                write()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                AnalysisLog.write("models gemma action=${action}_failed error=${error.javaClass.simpleName}")
                saveFailed.value = true
            } finally {
                inferenceSaving.value = false
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
