package minmul.memoir.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.edit
import minmul.memoir.core.storage.UserPreferencesKeys
import minmul.memoir.core.storage.userPreferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class UserPreferencesRepository @Inject constructor(
    @ApplicationContext context: Context,
) : OnboardingProgressStore {
    private val dataStore = context.userPreferencesDataStore

    override val onboardingProgress: Flow<Int> = dataStore.data.map { preferences ->
        preferences[UserPreferencesKeys.ONBOARDING_PROGRESS] ?: OnboardingProgress.LANDING
    }

    override suspend fun setOnboardingProgress(progress: Int) {
        dataStore.edit { preferences ->
            preferences[UserPreferencesKeys.ONBOARDING_PROGRESS] = progress
        }
    }

    override suspend fun normalizeOnboardingProgress() {
        dataStore.edit { preferences ->
            val current = preferences[UserPreferencesKeys.ONBOARDING_PROGRESS]
                ?: OnboardingProgress.LANDING
            preferences[UserPreferencesKeys.ONBOARDING_PROGRESS] =
                OnboardingProgress.normalize(current)
        }
    }
}
