package minmul.memoir.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import minmul.memoir.data.preferences.OnboardingProgress
import minmul.memoir.data.preferences.OnboardingProgressStore
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class RootViewModel @Inject constructor(
    private val onboardingProgressStore: OnboardingProgressStore,
) : ViewModel() {
    val onboardingProgress: StateFlow<Int?> = flow {
        onboardingProgressStore.normalizeOnboardingProgress()
        emitAll(onboardingProgressStore.onboardingProgress)
    }
        .map<Int, Int?> { it }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = null,
        )

    suspend fun setOnboardingProgress(progress: Int) {
        onboardingProgressStore.setOnboardingProgress(progress)
    }

    suspend fun completeOnboarding() {
        onboardingProgressStore.setOnboardingProgress(OnboardingProgress.COMPLETED)
    }

    suspend fun resetOnboarding() {
        onboardingProgressStore.setOnboardingProgress(OnboardingProgress.LANDING)
    }
}
