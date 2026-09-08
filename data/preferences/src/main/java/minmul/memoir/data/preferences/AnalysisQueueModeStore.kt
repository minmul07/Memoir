package minmul.memoir.data.preferences

import kotlinx.coroutines.flow.Flow
import minmul.memoir.core.model.AnalysisQueueMode

interface AnalysisQueueModeStore {
    val analysisQueueMode: Flow<AnalysisQueueMode>
    suspend fun setAnalysisQueueMode(mode: AnalysisQueueMode)
}
