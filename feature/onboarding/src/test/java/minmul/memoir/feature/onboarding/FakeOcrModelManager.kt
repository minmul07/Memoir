package minmul.memoir.feature.onboarding

import kotlinx.coroutines.flow.MutableStateFlow
import minmul.memoir.core.ai.OcrModelManager
import minmul.memoir.core.model.OcrModel
import minmul.memoir.core.model.OcrModelState
import minmul.memoir.core.model.OcrModelStatus

class FakeOcrModelManager : OcrModelManager {
    override val models =
        MutableStateFlow(OcrModel.entries.map { OcrModelState(it, OcrModelStatus.Missing) })
    val installed = mutableListOf<OcrModel>()
    var refreshCount = 0

    override suspend fun refresh(): List<OcrModelState> {
        refreshCount++
        return models.value
    }

    override suspend fun install(model: OcrModel) {
        installed += model
        models.value =
            models.value.map { if (it.model == model) it.copy(status = OcrModelStatus.Pending) else it }
    }
}
