package minmul.memoir.intake

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import minmul.memoir.data.preferences.OnboardingProgress
import minmul.memoir.data.preferences.OnboardingProgressStore

sealed interface IntakeUiState {
    data object Loading : IntakeUiState
    data object OpenOnboarding : IntakeUiState
    data object OpenQueue : IntakeUiState
    data object Finish : IntakeUiState
    data class Confirm(val imageUris: List<String>) : IntakeUiState
}

@HiltViewModel
class IntakeViewModel @Inject constructor(
    private val onboardingProgressStore: OnboardingProgressStore,
) : ViewModel() {
    private val receivedImageUris = MutableStateFlow<List<String>?>(null)
    private val userAction = MutableStateFlow<UserAction?>(null)

    private val onboardingProgress: StateFlow<Int?> = flow {
        onboardingProgressStore.normalizeOnboardingProgress()
        emitAll(onboardingProgressStore.onboardingProgress)
    }
        .map<Int, Int?> { it }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = null,
        )

    val uiState: StateFlow<IntakeUiState> = combine(
        onboardingProgress,
        receivedImageUris,
        userAction,
    ) { progress, imageUris, action ->
        when {
            progress == null || imageUris == null -> IntakeUiState.Loading
            !OnboardingProgress.isComplete(progress) -> IntakeUiState.OpenOnboarding
            imageUris.isEmpty() -> IntakeUiState.Finish
            action == UserAction.Add -> IntakeUiState.OpenQueue
            action == UserAction.Cancel -> IntakeUiState.Finish
            else -> IntakeUiState.Confirm(imageUris)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = IntakeUiState.Loading,
    )

    fun start(imageUris: List<String>) {
        receivedImageUris.compareAndSet(null, imageUris)
    }

    fun onAdd() {
        userAction.value = UserAction.Add
    }

    fun onCancel() {
        userAction.value = UserAction.Cancel
    }

    private enum class UserAction {
        Add,
        Cancel,
    }
}
