package minmul.memoir.data.preferences

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

sealed interface InformationCollectionReadState {
    val enabled: Boolean? get() = null

    data object Loading : InformationCollectionReadState
    data class Ready(override val enabled: Boolean) : InformationCollectionReadState
    data class Failed(val cause: Exception) : InformationCollectionReadState
}

/** App-lifetime observation shared by startup and settings. */
@Singleton
class InformationCollectionState @Inject constructor(
    private val preferences: InformationCollectionPreferencesStore,
) {
    private val mutableState =
        MutableStateFlow<InformationCollectionReadState>(InformationCollectionReadState.Loading)
    val state: StateFlow<InformationCollectionReadState> = mutableState.asStateFlow()
    private var started = false

    @Synchronized
    fun start(applicationScope: CoroutineScope) {
        if (started) return
        started = true
        applicationScope.launch {
            try {
                preferences.informationCollectionEnabled.collect { enabled ->
                    mutableState.value = InformationCollectionReadState.Ready(enabled)
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                mutableState.value = InformationCollectionReadState.Failed(error)
            }
        }
    }
}
