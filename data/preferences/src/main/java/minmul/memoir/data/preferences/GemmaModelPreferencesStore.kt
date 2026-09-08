package minmul.memoir.data.preferences

import kotlinx.coroutines.flow.Flow
import minmul.memoir.core.model.GemmaModel

interface GemmaModelPreferencesStore {
    val selectedGemmaModel: Flow<GemmaModel?>
    suspend fun setSelectedGemmaModel(model: GemmaModel)
}
