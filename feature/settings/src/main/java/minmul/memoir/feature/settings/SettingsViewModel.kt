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
import minmul.memoir.data.preferences.InformationCollectionPreferencesStore
import javax.inject.Inject

data class SettingsUiState(
    val analysisQueueMode: AnalysisQueueMode = AnalysisQueueMode.Manual,
    val preferencesLoaded: Boolean = false,
    val preferencesFailed: Boolean = false,
    val saving: Boolean = false,
    val informationCollectionEnabled: Boolean? = null,
    val informationCollectionFailed: Boolean = false,
    val informationCollectionSaving: Boolean = false,
)

private data class AnalysisQueueModeSelection(
    val mode: AnalysisQueueMode = AnalysisQueueMode.Manual,
    val loaded: Boolean = false,
    val failed: Boolean = false,
)

private data class InformationCollectionSelection(
    val enabled: Boolean? = null,
    val failed: Boolean = false,
)

private data class InformationCollectionWriteState(
    val saving: Boolean = false,
    val failed: Boolean = false,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferences: AnalysisQueueModeStore,
    private val informationCollectionPreferences: InformationCollectionPreferencesStore,
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
    private val informationCollectionWriteState =
        MutableStateFlow(InformationCollectionWriteState())
    private val informationCollectionSelection =
        informationCollectionPreferences.informationCollectionEnabled
            .map { InformationCollectionSelection(enabled = it) }
            .onStart { emit(InformationCollectionSelection()) }
            .catch {
                AnalysisLog.write("settings information_collection preferences_failed error=${it.javaClass.simpleName}")
                emit(InformationCollectionSelection(failed = true))
            }
    val state = combine(
        selection,
        saving,
        saveFailed,
        informationCollectionSelection,
        informationCollectionWriteState,
    ) { selection, saving, failed, collection, collectionWrite ->
        SettingsUiState(
            analysisQueueMode = selection.mode,
            preferencesLoaded = selection.loaded,
            preferencesFailed = selection.failed || failed,
            saving = saving,
            informationCollectionEnabled = collection.enabled,
            informationCollectionFailed = collection.failed || collectionWrite.failed,
            informationCollectionSaving = collectionWrite.saving,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, SettingsUiState())

    fun setInformationCollectionEnabled(enabled: Boolean) {
        val current = state.value.informationCollectionEnabled ?: return
        if (informationCollectionWriteState.value.saving || current == enabled) return
        informationCollectionWriteState.value = InformationCollectionWriteState(saving = true)
        viewModelScope.launch {
            try {
                informationCollectionPreferences.setInformationCollectionEnabled(enabled)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                AnalysisLog.write(
                    "settings information_collection save_failed error=${error.javaClass.simpleName}",
                )
                informationCollectionWriteState.value =
                    InformationCollectionWriteState(failed = true)
            } finally {
                informationCollectionWriteState.value =
                    informationCollectionWriteState.value.copy(saving = false)
            }
        }
    }

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
