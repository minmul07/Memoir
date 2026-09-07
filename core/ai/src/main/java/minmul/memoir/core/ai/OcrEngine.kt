package minmul.memoir.core.ai

import kotlinx.coroutines.flow.StateFlow
import minmul.memoir.core.model.OcrModel
import minmul.memoir.core.model.OcrModelState

interface OcrEngine {
    suspend fun recognize(imagePath: String): String?
}

interface OcrModelManager {
    val models: StateFlow<List<OcrModelState>>

    /** Returns this check's snapshot, independent of subsequent UI refreshes. */
    suspend fun refresh(): List<OcrModelState>

    /** Joins an existing download of this model. Caller cancellation does not stop the download. */
    suspend fun install(model: OcrModel)
}
