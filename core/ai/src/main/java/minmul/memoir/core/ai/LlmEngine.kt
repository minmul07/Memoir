package minmul.memoir.core.ai

import kotlinx.coroutines.flow.StateFlow
import minmul.memoir.core.model.GemmaModel
import minmul.memoir.core.model.LlmRuntimeStatus
import java.io.File

interface LlmEngine {
    val status: StateFlow<LlmRuntimeStatus>
    fun markIdle()
    fun markMissing()
    suspend fun load(model: GemmaModel, file: File)
    suspend fun summarize(imagePath: String, ocrText: String?): String
    suspend fun close()
}
