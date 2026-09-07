package minmul.memoir.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import minmul.memoir.core.model.OcrModel
import minmul.memoir.core.storage.UserPreferencesKeys
import minmul.memoir.core.storage.userPreferencesDataStore
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserPreferencesRepository internal constructor(
    private val dataStore: DataStore<Preferences>,
) : OnboardingProgressStore, OcrModelPreferencesStore {
    @Inject
    constructor(@ApplicationContext context: Context) : this(context.userPreferencesDataStore)

    override val disabledOcrModels: Flow<Set<OcrModel>> = dataStore.data.map { preferences ->
        val disabled = preferences[UserPreferencesKeys.DISABLED_OCR_MODELS].orEmpty()
        OcrModel.entries.filterTo(mutableSetOf()) { it.name in disabled }
    }

    override suspend fun setOcrModelEnabled(model: OcrModel, enabled: Boolean) {
        dataStore.edit { preferences ->
            val disabled = preferences[UserPreferencesKeys.DISABLED_OCR_MODELS].orEmpty()
            preferences[UserPreferencesKeys.DISABLED_OCR_MODELS] =
                if (enabled) disabled - model.name else disabled + model.name
        }
    }

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
