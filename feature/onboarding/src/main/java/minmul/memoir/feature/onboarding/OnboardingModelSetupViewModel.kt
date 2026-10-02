package minmul.memoir.feature.onboarding

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
import minmul.memoir.core.ai.OcrModelManager
import minmul.memoir.core.model.GemmaModel
import minmul.memoir.core.model.GemmaModelState
import minmul.memoir.core.model.GemmaModelStatus
import minmul.memoir.core.model.OcrModel
import minmul.memoir.core.model.OcrModelState
import minmul.memoir.data.model.GemmaModelStore
import minmul.memoir.data.preferences.GemmaModelPreferencesStore
import javax.inject.Inject

data class OnboardingModelSetupState(
    val gemmaModels: List<GemmaModelState> = GemmaModel.entries.map { GemmaModelState(it) },
    val ocrModels: List<OcrModelState> = OcrModel.entries.map { OcrModelState(it) },
    val preferencesLoaded: Boolean = false,
    val preferencesFailed: Boolean = false,
    val savingModels: Set<GemmaModel> = emptySet(),
    val refreshFailed: Boolean = false,
)

private data class GemmaSelection(
    val selected: GemmaModel? = null,
    val loaded: Boolean = false,
    val failed: Boolean = false,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class OnboardingModelSetupViewModel @Inject constructor(
    private val gemma: GemmaModelStore,
    private val ocr: OcrModelManager,
    private val preferences: GemmaModelPreferencesStore,
) : ViewModel() {
    private val reload = MutableStateFlow(0)
    private val saving = MutableStateFlow<Set<GemmaModel>>(emptySet())
    private val saveFailed = MutableStateFlow(false)
    private val refreshFailed = MutableStateFlow(false)
    private var refreshJob: Job? = null
    private var autoSelectAttempted = false
    private val selection = reload.flatMapLatest {
        preferences.selectedGemmaModel
            .map { GemmaSelection(selected = it, loaded = true) }
            .onStart { emit(GemmaSelection()) }
            .catch {
                AnalysisLog.write("models gemma onboarding preferences_failed error=${it.javaClass.simpleName}")
                emit(GemmaSelection(failed = true))
            }
    }
    val state = combine(
        gemma.models,
        ocr.models,
        selection,
        saving,
        combine(saveFailed, refreshFailed) { save, refresh -> save to refresh },
    ) { gemmaModels, ocrModels, selection, saving, errors ->
        OnboardingModelSetupState(
            gemmaModels = gemmaModels.map {
                it.copy(selected = it.model == selection.selected && it.status == GemmaModelStatus.Ready)
            },
            ocrModels = ocrModels,
            preferencesLoaded = selection.loaded,
            preferencesFailed = selection.failed || errors.first,
            savingModels = saving,
            refreshFailed = errors.second,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, OnboardingModelSetupState())

    init {
        combine(gemma.models, selection) { models, selection -> models to selection }
            .onEach { (models, selection) -> maybeAutoSelect(models, selection) }
            .launchIn(viewModelScope)
    }

    fun refresh() {
        if (refreshJob?.isActive == true) return
        reload.update { it + 1 }
        saveFailed.value = false
        refreshFailed.value = false
        autoSelectAttempted = false
        refreshJob = viewModelScope.launch {
            // Refresh independently so a failure in one family does not hide the other.
            launch { refreshModels { gemma.refresh() } }
            launch { refreshModels { ocr.refresh() } }
        }
    }

    fun installGemma(model: GemmaModel) = modelAction("gemma install model=$model") {
        gemma.install(model)
    }

    fun cancelGemma(model: GemmaModel) = modelAction("gemma cancel model=$model") {
        gemma.cancel(model)
    }

    fun installOcr(model: OcrModel) = modelAction("ocr install model=$model") {
        ocr.install(model)
    }

    fun selectGemma(model: GemmaModel) {
        if (!state.value.preferencesLoaded || saving.value.isNotEmpty()) return
        if (state.value.gemmaModels.none { it.model == model && it.status == GemmaModelStatus.Ready }) return
        if (state.value.gemmaModels.any { it.model == model && it.selected }) return
        saving.value = setOf(model)
        saveFailed.value = false
        viewModelScope.launch {
            try {
                preferences.setSelectedGemmaModel(model)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                saveFailed.value = true
                AnalysisLog.write("models gemma onboarding select_failed model=$model error=${error.javaClass.simpleName}")
            } finally {
                saving.value = emptySet()
            }
        }
    }

    private suspend fun maybeAutoSelect(models: List<GemmaModelState>, selection: GemmaSelection) {
        if (!selection.loaded || selection.failed || autoSelectAttempted || saving.value.isNotEmpty()) return
        val stored = models.firstOrNull { it.model == selection.selected }
        if (stored != null && (stored.status == GemmaModelStatus.Checking || stored.isDownloading)) return
        if (stored?.status == GemmaModelStatus.Ready) return
        val ready = models.firstOrNull { it.status == GemmaModelStatus.Ready } ?: return
        autoSelectAttempted = true
        saving.value = setOf(ready.model)
        try {
            preferences.setSelectedGemmaModel(ready.model)
        } catch (cancelled: CancellationException) {
            autoSelectAttempted = false
            throw cancelled
        } catch (error: Exception) {
            saveFailed.value = true
            AnalysisLog.write("models gemma onboarding auto_select_failed model=${ready.model} error=${error.javaClass.simpleName}")
        } finally {
            saving.value = emptySet()
        }
    }

    private fun modelAction(action: String, block: suspend () -> Unit) {
        AnalysisLog.write("models onboarding action=$action")
        viewModelScope.launch {
            try {
                block()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                // Shared download managers publish the model's failure state.
                AnalysisLog.write("models onboarding action_failed=$action error=${error.javaClass.simpleName}")
            }
        }
    }

    private suspend fun refreshModels(block: suspend () -> Unit) {
        try {
            block()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            refreshFailed.value = true
            AnalysisLog.write("models onboarding refresh_failed error=${error.javaClass.simpleName}")
        }
    }
}
