package minmul.memoir.data.preferences

import kotlinx.coroutines.flow.Flow

interface OnboardingProgressStore {
    val onboardingProgress: Flow<Int>

    suspend fun setOnboardingProgress(progress: Int)

    suspend fun normalizeOnboardingProgress()
}
