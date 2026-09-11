package minmul.memoir.data.preferences

import kotlinx.coroutines.flow.Flow
import minmul.memoir.core.model.GemmaInferenceSettings
import minmul.memoir.core.model.GemmaModel

interface GemmaModelPreferencesStore {
    val selectedGemmaModel: Flow<GemmaModel?>
    suspend fun setSelectedGemmaModel(model: GemmaModel)
    val inferenceSettings: Flow<GemmaInferenceSettings>
    suspend fun setMaxOutputToken(value: Int)
    suspend fun setTopK(value: Int)
    suspend fun setTopP(value: Double)
    suspend fun setTemperature(value: Double)
    suspend fun setThinkingEnabled(enabled: Boolean)
    suspend fun setSpeculativeDecodingEnabled(enabled: Boolean)
}
