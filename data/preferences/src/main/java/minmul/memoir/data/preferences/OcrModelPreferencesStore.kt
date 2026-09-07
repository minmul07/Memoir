package minmul.memoir.data.preferences

import kotlinx.coroutines.flow.Flow
import minmul.memoir.core.model.OcrModel

interface OcrModelPreferencesStore {
    val disabledOcrModels: Flow<Set<OcrModel>>
    suspend fun setOcrModelEnabled(model: OcrModel, enabled: Boolean)
}
