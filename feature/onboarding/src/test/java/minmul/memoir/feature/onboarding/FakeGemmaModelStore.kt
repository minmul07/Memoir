package minmul.memoir.feature.onboarding

import kotlinx.coroutines.flow.MutableStateFlow
import minmul.memoir.core.model.GemmaModel
import minmul.memoir.core.model.GemmaModelState
import minmul.memoir.core.model.GemmaModelStatus
import minmul.memoir.data.model.GemmaModelStore
import java.io.File

class FakeGemmaModelStore : GemmaModelStore {
    override val models =
        MutableStateFlow(GemmaModel.entries.map { GemmaModelState(it, GemmaModelStatus.Missing) })
    val installed = mutableListOf<GemmaModel>()
    val cancelled = mutableListOf<GemmaModel>()
    var refreshError: Exception? = null
    var refreshCount = 0

    fun setStatus(model: GemmaModel, status: GemmaModelStatus) {
        models.value = models.value.map { if (it.model == model) it.copy(status = status) else it }
    }

    override suspend fun refresh(): List<GemmaModelState> {
        refreshCount++
        refreshError?.let { throw it }
        return models.value
    }

    override suspend fun install(model: GemmaModel) {
        installed += model
        setStatus(model, GemmaModelStatus.Pending)
    }

    override suspend fun cancel(model: GemmaModel) {
        cancelled += model
        setStatus(model, GemmaModelStatus.Failed)
    }

    override suspend fun delete(model: GemmaModel) = error("Onboarding must not delete models")
    override fun installedFile(model: GemmaModel): File? = null
}
