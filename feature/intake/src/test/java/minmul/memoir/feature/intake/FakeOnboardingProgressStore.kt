package minmul.memoir.feature.intake

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import minmul.memoir.data.preferences.OnboardingProgress
import minmul.memoir.data.preferences.OnboardingProgressStore

class FakeOnboardingProgressStore(
    initialProgress: Int = OnboardingProgress.LANDING,
) : OnboardingProgressStore {
    private val progress = MutableStateFlow(initialProgress)

    override val onboardingProgress: Flow<Int> = progress

    override suspend fun setOnboardingProgress(progress: Int) {
        this.progress.value = progress
    }

    override suspend fun normalizeOnboardingProgress() {
        this.progress.value = OnboardingProgress.normalize(this.progress.value)
    }
}
