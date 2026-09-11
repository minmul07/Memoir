package minmul.memoir.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import minmul.memoir.core.model.AnalysisQueueMode
import minmul.memoir.core.model.GemmaInferenceSettings
import minmul.memoir.core.model.GemmaModel
import minmul.memoir.core.model.OcrModel
import minmul.memoir.core.storage.UserPreferencesKeys
import minmul.memoir.core.storage.userPreferencesDataStore
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserPreferencesRepository internal constructor(
    private val dataStore: DataStore<Preferences>,
) : OnboardingProgressStore, OcrModelPreferencesStore, GemmaModelPreferencesStore,
    AnalysisQueueModeStore {
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

    override val selectedGemmaModel: Flow<GemmaModel?> = dataStore.data.map { preferences ->
        val stored = preferences[UserPreferencesKeys.SELECTED_GEMMA_MODEL] ?: return@map null
        GemmaModel.entries.firstOrNull { it.name == stored }
    }

    override suspend fun setSelectedGemmaModel(model: GemmaModel) {
        dataStore.edit { preferences ->
            preferences[UserPreferencesKeys.SELECTED_GEMMA_MODEL] = model.name
        }
    }

    override val inferenceSettings: Flow<GemmaInferenceSettings> =
        dataStore.data.map { preferences ->
            GemmaInferenceSettings.clamp(
                maxOutputToken = preferences[UserPreferencesKeys.GEMMA_MAX_OUTPUT_TOKEN]
                    ?: GemmaInferenceSettings.DEFAULT_MAX_OUTPUT_TOKEN,
                topK = preferences[UserPreferencesKeys.GEMMA_TOP_K]
                    ?: GemmaInferenceSettings.DEFAULT_TOP_K,
                thinkingEnabled = preferences[UserPreferencesKeys.GEMMA_THINKING_ENABLED]
                    ?: GemmaInferenceSettings.DEFAULT_THINKING_ENABLED,
                topP = preferences[UserPreferencesKeys.GEMMA_TOP_P]
                    ?: GemmaInferenceSettings.DEFAULT_TOP_P,
                temperature = preferences[UserPreferencesKeys.GEMMA_TEMPERATURE]
                    ?: GemmaInferenceSettings.DEFAULT_TEMPERATURE,
                speculativeDecodingEnabled = preferences[UserPreferencesKeys.GEMMA_SPECULATIVE_DECODING]
                    ?: GemmaInferenceSettings.DEFAULT_SPECULATIVE_DECODING,
            )
        }

    override suspend fun setMaxOutputToken(value: Int) {
        val settings = GemmaInferenceSettings.clamp(maxOutputToken = value)
        dataStore.edit { preferences ->
            preferences[UserPreferencesKeys.GEMMA_MAX_OUTPUT_TOKEN] = settings.maxOutputToken
        }
    }

    override suspend fun setTopK(value: Int) {
        val settings = GemmaInferenceSettings.clamp(topK = value)
        dataStore.edit { preferences ->
            preferences[UserPreferencesKeys.GEMMA_TOP_K] = settings.topK
        }
    }

    override suspend fun setTopP(value: Double) {
        val settings = GemmaInferenceSettings.clamp(topP = value)
        dataStore.edit { preferences ->
            preferences[UserPreferencesKeys.GEMMA_TOP_P] = settings.topP
        }
    }

    override suspend fun setTemperature(value: Double) {
        val settings = GemmaInferenceSettings.clamp(temperature = value)
        dataStore.edit { preferences ->
            preferences[UserPreferencesKeys.GEMMA_TEMPERATURE] = settings.temperature
        }
    }

    override suspend fun setThinkingEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[UserPreferencesKeys.GEMMA_THINKING_ENABLED] = enabled
        }
    }

    override suspend fun setSpeculativeDecodingEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[UserPreferencesKeys.GEMMA_SPECULATIVE_DECODING] = enabled
        }
    }

    override val analysisQueueMode: Flow<AnalysisQueueMode> = dataStore.data.map { preferences ->
        val stored = preferences[UserPreferencesKeys.ANALYSIS_QUEUE_MODE]
        AnalysisQueueMode.entries.firstOrNull { it.name == stored } ?: AnalysisQueueMode.Manual
    }

    override suspend fun setAnalysisQueueMode(mode: AnalysisQueueMode) {
        dataStore.edit { preferences ->
            preferences[UserPreferencesKeys.ANALYSIS_QUEUE_MODE] = mode.name
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
