package minmul.memoir.core.ai

import kotlinx.coroutines.CompletableDeferred
import minmul.memoir.core.model.OcrModel
import minmul.memoir.core.model.OcrModelState

internal class FakeOcrModelBackend : OcrModelBackend {
    val available = mutableSetOf<OcrModel>()
    val checkFailures = mutableSetOf<OcrModel>()
    val installFailures = mutableSetOf<OcrModel>()
    val downloads = mutableListOf<OcrModel>()
    val gates = mutableMapOf<OcrModel, CompletableDeferred<Unit>>()
    val progress = mutableMapOf<OcrModel, (OcrModelState) -> Unit>()

    override suspend fun isAvailable(model: OcrModel): Boolean {
        check(model !in checkFailures) { "availability_failed" }
        return model in available
    }

    override suspend fun install(model: OcrModel, onProgress: (OcrModelState) -> Unit) {
        downloads += model
        progress[model] = onProgress
        gates[model]?.await()
        check(model !in installFailures) { "download_failed" }
        available += model
    }
}
