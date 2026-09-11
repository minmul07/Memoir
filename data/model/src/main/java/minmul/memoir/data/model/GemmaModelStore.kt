package minmul.memoir.data.model

import kotlinx.coroutines.flow.StateFlow
import minmul.memoir.core.model.GemmaModel
import minmul.memoir.core.model.GemmaModelState
import java.io.File

interface GemmaModelStore {
    val models: StateFlow<List<GemmaModelState>>

    /** Returns this check's snapshot, independent of subsequent UI refreshes. */
    suspend fun refresh(): List<GemmaModelState>

    /** Joins an existing download of this model. Caller cancellation does not stop the download. */
    suspend fun install(model: GemmaModel)

    suspend fun cancel(model: GemmaModel)

    suspend fun delete(model: GemmaModel)

    fun installedFile(model: GemmaModel): File?
}
