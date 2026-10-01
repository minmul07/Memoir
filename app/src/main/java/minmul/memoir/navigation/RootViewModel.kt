package minmul.memoir.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import minmul.memoir.data.preferences.InformationCollectionPreferencesStore
import minmul.memoir.data.preferences.OnboardingProgress
import minmul.memoir.data.preferences.OnboardingProgressStore
import java.io.IOException
import javax.inject.Inject

@HiltViewModel
class RootViewModel @Inject constructor(
    private val onboardingProgressStore: OnboardingProgressStore,
    private val informationCollectionPreferencesStore: InformationCollectionPreferencesStore,
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

    suspend fun declineCrashlytics(): Boolean {
        return try {
            informationCollectionPreferencesStore.setInformationCollectionEnabled(false)
            onboardingProgressStore.setOnboardingProgress(OnboardingProgress.MODEL_SETUP)
            true
        } catch (_: IOException) {
            false
        }
    }

    suspend fun resetOnboarding() {
        onboardingProgressStore.setOnboardingProgress(OnboardingProgress.LANDING)
    }
}
