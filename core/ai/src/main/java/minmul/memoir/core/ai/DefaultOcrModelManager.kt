package minmul.memoir.core.ai

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import minmul.memoir.core.model.OcrModel
import minmul.memoir.core.model.OcrModelState
import minmul.memoir.core.model.OcrModelStatus

internal interface OcrModelBackend {
    suspend fun isAvailable(model: OcrModel): Boolean
    suspend fun install(model: OcrModel, onProgress: (OcrModelState) -> Unit)
}

/** Application-scoped downloads are shared by settings and analysis, independently per model. */
internal class DefaultOcrModelManager(
    private val backend: OcrModelBackend,
    private val scope: CoroutineScope,
    private val log: (String) -> Unit = {},
) : OcrModelManager {
    private val mutableModels = MutableStateFlow(OcrModel.entries.map { OcrModelState(it) })
    override val models = mutableModels.asStateFlow()

    private class Operation {
        val lock = Mutex()
        var download: Deferred<Result<Unit>>? = null
    }

    private val operations = OcrModel.entries.associateWith { Operation() }

    override suspend fun refresh(): List<OcrModelState> = coroutineScope {
        OcrModel.entries.map { model ->
            async {
                val operation = operations.getValue(model)
                operation.lock.withLock {
                    if (operation.download?.isActive == true) {
                        return@withLock models.value.first { it.model == model }
                    }
                    val previous = models.value.first { it.model == model }
                    set(OcrModelState(model))
                    try {
                        val ready = backend.isAvailable(model)
                        val state = OcrModelState(
                            model,
                            if (ready) OcrModelStatus.Ready else OcrModelStatus.Missing
                        )
                        set(state)
                        log("models model=$model available=$ready")
                        state
                    } catch (cancelled: CancellationException) {
                        set(previous)
                        throw cancelled
                    } catch (error: Exception) {
                        val state = OcrModelState(model, OcrModelStatus.Failed)
                        set(state)
                        log("models model=$model check_failed error=${error.javaClass.simpleName}")
                        state
                    }
                }
            }
        }.awaitAll()
    }

    override suspend fun install(model: OcrModel) {
        val operation = operations.getValue(model)
        val download = operation.lock.withLock {
            operation.download?.takeIf { it.isActive } ?: scope.async(start = CoroutineStart.LAZY) {
                try {
                    if (!backend.isAvailable(model)) {
                        log("models model=$model download_begin")
                        backend.install(model) { progress ->
                            set(progress)
                            log("models model=$model status=${progress.status} bytes=${progress.downloadedBytes}/${progress.totalBytes}")
                        }
                        check(backend.isAvailable(model)) { "ocr_model_not_available_after_install" }
                    }
                    set(OcrModelState(model, OcrModelStatus.Ready))
                    log("models model=$model ready")
                    Result.success(Unit)
                } catch (cancelled: CancellationException) {
                    set(OcrModelState(model, OcrModelStatus.Failed))
                    throw cancelled
                } catch (error: Exception) {
                    set(OcrModelState(model, OcrModelStatus.Failed))
                    log("models model=$model download_failed error=${error.javaClass.simpleName}")
                    Result.failure(error)
                }
            }.also {
                operation.download = it
                set(OcrModelState(model, OcrModelStatus.Pending))
                it.start()
            }
        }
        download.await().getOrThrow()
    }

    private fun set(state: OcrModelState) {
        mutableModels.update { states -> states.map { if (it.model == state.model) state else it } }
    }
}
