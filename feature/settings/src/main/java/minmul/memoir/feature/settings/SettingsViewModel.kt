package minmul.memoir.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import minmul.memoir.core.ai.AnalysisLog
import minmul.memoir.core.model.AnalysisQueueMode
import minmul.memoir.data.preferences.AnalysisQueueModeStore
import javax.inject.Inject

data class SettingsUiState(
    val analysisQueueMode: AnalysisQueueMode = AnalysisQueueMode.Immediate,
    val preferencesLoaded: Boolean = false,
    val preferencesFailed: Boolean = false,
    val saving: Boolean = false,
)

private data class AnalysisQueueModeSelection(
    val mode: AnalysisQueueMode = AnalysisQueueMode.Immediate,
    val loaded: Boolean = false,
    val failed: Boolean = false,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferences: AnalysisQueueModeStore,
) : ViewModel() {
    private val saving = MutableStateFlow(false)
    private val saveFailed = MutableStateFlow(false)
    private val selection = preferences.analysisQueueMode
        .map { AnalysisQueueModeSelection(mode = it, loaded = true) }
        .onStart { emit(AnalysisQueueModeSelection()) }
        .catch {
            AnalysisLog.write("settings analysis_queue_mode preferences_failed error=${it.javaClass.simpleName}")
            emit(AnalysisQueueModeSelection(failed = true))
        }
    val state = combine(selection, saving, saveFailed) { selection, saving, failed ->
        SettingsUiState(
            analysisQueueMode = selection.mode,
            preferencesLoaded = selection.loaded,
            preferencesFailed = selection.failed || failed,
            saving = saving,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, SettingsUiState())

    fun setAnalysisQueueMode(mode: AnalysisQueueMode) {
        if (!state.value.preferencesLoaded || saving.value) return
        if (mode == state.value.analysisQueueMode) return
        AnalysisLog.write("settings analysis_queue_mode action=select mode=$mode")
        saving.value = true
        saveFailed.value = false
        viewModelScope.launch {
            try {
                preferences.setAnalysisQueueMode(mode)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                AnalysisLog.write(
                    "settings analysis_queue_mode action=select_failed mode=$mode error=${error.javaClass.simpleName}",
                )
                saveFailed.value = true
            } finally {
                saving.value = false
            }
        }
    }
}
