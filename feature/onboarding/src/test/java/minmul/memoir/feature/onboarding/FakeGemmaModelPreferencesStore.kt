package minmul.memoir.feature.onboarding

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import minmul.memoir.core.model.GemmaInferenceSettings
import minmul.memoir.core.model.GemmaModel
import minmul.memoir.data.preferences.GemmaModelPreferencesStore

class FakeGemmaModelPreferencesStore : GemmaModelPreferencesStore {
    val selected = MutableStateFlow<GemmaModel?>(null)
    var readError: Exception? = null
    var saveError: Exception? = null
    override val selectedGemmaModel: Flow<GemmaModel?> = flow {
        readError?.let { throw it }
        selected.collect { emit(it) }
    }
    override val inferenceSettings = MutableStateFlow(GemmaInferenceSettings())

    override suspend fun setSelectedGemmaModel(model: GemmaModel) {
        saveError?.let { throw it }
        selected.value = model
    }

    override suspend fun setMaxOutputToken(value: Int) = error("Not used in onboarding")
    override suspend fun setTopK(value: Int) = error("Not used in onboarding")
    override suspend fun setTopP(value: Double) = error("Not used in onboarding")
    override suspend fun setTemperature(value: Double) = error("Not used in onboarding")
    override suspend fun setThinkingEnabled(enabled: Boolean) = error("Not used in onboarding")
    override suspend fun setSpeculativeDecodingEnabled(enabled: Boolean) =
        error("Not used in onboarding")
}
