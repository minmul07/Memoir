package minmul.memoir.data.model

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import minmul.memoir.core.model.GemmaModel
import minmul.memoir.core.model.GemmaModelState
import minmul.memoir.core.model.GemmaModelStatus
import java.io.File

/** Application-scoped downloads are shared independently per model. */
class DefaultGemmaModelStore(
    private val engine: GemmaDownloadEngine,
    private val files: GemmaModelFiles,
    private val ids: GemmaDownloadIdStore,
    private val scope: CoroutineScope,
    private val title: (GemmaModel) -> String = { it.name },
    private val log: (String) -> Unit = {},
) : GemmaModelStore {
    private val mutableModels = MutableStateFlow(GemmaModel.entries.map { GemmaModelState(it) })
    override val models = mutableModels.asStateFlow()

    private class Operation {
        val lock = Mutex()
        var download: Deferred<Result<Unit>>? = null
    }

    private val operations = GemmaModel.entries.associateWith { Operation() }

    override fun installedFile(model: GemmaModel): File? = files.installedFile(model)

    override suspend fun refresh(): List<GemmaModelState> = coroutineScope {
        GemmaModel.entries.map { model ->
            async {
                val operation = operations.getValue(model)
                operation.lock.withLock {
                    if (operation.download?.isActive == true) {
                        return@withLock models.value.first { it.model == model }
                    }
                    val previous = models.value.first { it.model == model }
                    set(GemmaModelState(model))
                    try {
                        inspect(model, operation)
                    } catch (cancelled: CancellationException) {
                        set(previous)
                        throw cancelled
                    } catch (error: Exception) {
                        val state = GemmaModelState(model, GemmaModelStatus.Failed)
                        set(state)
                        log("models gemma model=$model check_failed error=${error.javaClass.simpleName}")
                        state
                    }
                }
            }
        }.awaitAll()
    }

    override suspend fun install(model: GemmaModel) {
        val download = ensureDownload(model)
        download.await().getOrThrow()
    }

    override suspend fun cancel(model: GemmaModel) {
        val operation = operations.getValue(model)
        operation.lock.withLock {
            val id = ids.get(model)
            if (id != null) {
                engine.remove(id)
                ids.clear(model)
            }
            files.delete(model)
            operation.download?.cancel()
            operation.download = null
            set(GemmaModelState(model, GemmaModelStatus.Failed))
            log("models gemma model=$model cancelled downloadId=$id")
        }
    }

    override suspend fun delete(model: GemmaModel) {
        val operation = operations.getValue(model)
        operation.lock.withLock {
            if (operation.download?.isActive == true) {
                log("models gemma model=$model delete_skipped reason=downloading")
                return
            }
            if (models.value.none { it.model == model && it.status == GemmaModelStatus.Ready }) {
                log("models gemma model=$model delete_skipped reason=not_ready")
                return
            }
            files.delete(model)
            ids.clear(model)
            set(GemmaModelState(model, GemmaModelStatus.Missing))
            log("models gemma model=$model deleted")
        }
    }

    private suspend fun inspect(model: GemmaModel, operation: Operation): GemmaModelState {
        val id = ids.get(model)
        if (id != null) {
            val snapshot = engine.query(id)
            when {
                snapshot == null -> {
                    log("models gemma model=$model download_missing downloadId=$id")
                    ids.clear(model)
                }

                snapshot.status.isActive -> {
                    log(
                        "models gemma model=$model reconnect downloadId=$id status=${snapshot.status} bytes=${snapshot.downloadedBytes}/${snapshot.totalBytes}",
                    )
                    startDownloadLocked(model, operation)
                    return models.value.first { it.model == model }
                }

                snapshot.status == GemmaDownloadStatus.Successful -> {
                    if (files.isReady(model)) {
                        ids.clear(model)
                        val state = GemmaModelState(model, GemmaModelStatus.Ready)
                        set(state)
                        log("models gemma model=$model available=true")
                        return state
                    }
                    ids.clear(model)
                    files.delete(model)
                    val state = GemmaModelState(model, GemmaModelStatus.Failed)
                    set(state)
                    log("models gemma model=$model file_invalid")
                    return state
                }

                else -> {
                    ids.clear(model)
                    files.delete(model)
                    val state = GemmaModelState(model, GemmaModelStatus.Failed)
                    set(state)
                    log("models gemma model=$model download_failed")
                    return state
                }
            }
        }
        return if (files.isReady(model)) {
            val state = GemmaModelState(model, GemmaModelStatus.Ready)
            set(state)
            log("models gemma model=$model available=true")
            state
        } else {
            files.delete(model)
            val state = GemmaModelState(model, GemmaModelStatus.Missing)
            set(state)
            log("models gemma model=$model available=false")
            state
        }
    }

    private suspend fun ensureDownload(model: GemmaModel): Deferred<Result<Unit>> {
        val operation = operations.getValue(model)
        return operation.lock.withLock {
            operation.download?.takeIf { it.isActive } ?: startDownloadLocked(model, operation)
        }
    }

    private fun startDownloadLocked(
        model: GemmaModel,
        operation: Operation
    ): Deferred<Result<Unit>> {
        return scope.async(start = CoroutineStart.LAZY) {
            try {
                runDownload(model)
                Result.success(Unit)
            } catch (cancelled: CancellationException) {
                set(GemmaModelState(model, GemmaModelStatus.Failed))
                throw cancelled
            } catch (error: Exception) {
                set(GemmaModelState(model, GemmaModelStatus.Failed))
                log("models gemma model=$model download_failed error=${error.javaClass.simpleName}")
                Result.failure(error)
            }
        }.also {
            operation.download = it
            set(GemmaModelState(model, GemmaModelStatus.Pending))
            log("models gemma model=$model download_begin")
            it.start()
        }
    }

    private suspend fun runDownload(model: GemmaModel) {
        val existingId = ids.get(model)
        val existing = existingId?.let { engine.query(it) }
        when {
            existing != null && existing.status.isActive -> {
                log(
                    "models gemma model=$model reconnect downloadId=$existingId status=${existing.status} bytes=${existing.downloadedBytes}/${existing.totalBytes}",
                )
                awaitCompletion(model, existingId)
            }

            existing?.status == GemmaDownloadStatus.Successful && files.isReady(model) -> {
                ids.clear(model)
                set(GemmaModelState(model, GemmaModelStatus.Ready))
                log("models gemma model=$model ready")
            }

            existing == null && files.isReady(model) -> {
                ids.clear(model)
                set(GemmaModelState(model, GemmaModelStatus.Ready))
                log("models gemma model=$model ready")
            }

            else -> {
                files.delete(model)
                files.ensureDirectory()
                currentCoroutineContext().ensureActive()
                val enqueued = engine.enqueue(model, files.destination(model), title(model))
                try {
                    ids.set(model, enqueued)
                } catch (cancelled: CancellationException) {
                    engine.remove(enqueued)
                    throw cancelled
                }
                log("models gemma model=$model enqueue downloadId=$enqueued")
                awaitCompletion(model, enqueued)
            }
        }
    }

    private suspend fun awaitCompletion(model: GemmaModel, id: Long) {
        val terminal = engine.watch(id)
            .onEach { snapshot ->
                if (!snapshot.status.isTerminal) {
                    val state = progressState(model, snapshot)
                    set(state)
                    log(
                        "models gemma model=$model status=${state.status} bytes=${state.downloadedBytes}/${state.totalBytes}",
                    )
                }
            }
            .first { it.status.isTerminal }
        when (terminal.status) {
            GemmaDownloadStatus.Successful -> {
                check(files.isReady(model)) { "gemma_file_invalid" }
                ids.clear(model)
                set(GemmaModelState(model, GemmaModelStatus.Ready))
                log("models gemma model=$model ready")
            }

            else -> {
                ids.clear(model)
                files.delete(model)
                log("models gemma model=$model download_failed status=${terminal.status}")
                error("gemma_download_failed")
            }
        }
    }

    private fun progressState(model: GemmaModel, snapshot: GemmaDownloadSnapshot) = GemmaModelState(
        model = model,
        status = when (snapshot.status) {
            GemmaDownloadStatus.Pending -> GemmaModelStatus.Pending
            GemmaDownloadStatus.Running -> GemmaModelStatus.Downloading
            GemmaDownloadStatus.Paused -> GemmaModelStatus.Paused
            GemmaDownloadStatus.Successful -> GemmaModelStatus.Downloading
            GemmaDownloadStatus.Failed -> GemmaModelStatus.Failed
        },
        downloadedBytes = snapshot.downloadedBytes,
        totalBytes = snapshot.totalBytes,
    )

    private fun set(state: GemmaModelState) {
        mutableModels.update { states -> states.map { if (it.model == state.model) state else it } }
    }
}
