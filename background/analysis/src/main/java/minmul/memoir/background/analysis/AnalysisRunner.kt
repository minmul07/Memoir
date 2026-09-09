package minmul.memoir.background.analysis

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import minmul.memoir.core.ai.LlmEngine
import minmul.memoir.core.ai.OcrEngine
import minmul.memoir.core.model.JobStage
import minmul.memoir.data.content.AnalysisRepository
import minmul.memoir.data.content.ContentRepository
import java.io.File

class AnalysisRunner(
    private val repository: AnalysisRepository,
    private val content: ContentRepository,
    private val ocr: OcrEngine,
    private val llm: LlmEngine,
    private val models: ReadyGemmaModelLocator,
    private val imageFile: (String) -> File,
    private val log: (String) -> Unit = {},
) {
    private val mutex = Mutex()

    suspend fun drain() = mutex.withLock {
        log("queue drain_start recovery_begin")
        repository.recoverInterrupted()
        log("queue recovery_complete")
        if (!repository.hasQueuedWork()) {
            llm.markIdle()
            log("queue drain_end empty")
            return@withLock
        }
        val located = models.locate()
        if (located == null) {
            llm.markMissing()
            log("queue drain_end model_missing")
            return@withLock
        }
        try {
            log("queue model_load model=${located.model.name}")
            llm.load(located.model, located.file)
            log("queue model_ready model=${located.model.name}")
        } catch (cancelled: CancellationException) {
            llm.close()
            throw cancelled
        } catch (error: Exception) {
            log("queue model_load_failed error=${error.javaClass.simpleName}")
            repository.failActiveQueue("model_load_failed")
            log("queue drain_end model_load_failed")
            return@withLock
        }
        try {
            while (currentCoroutineContext().isActive) {
                val item = repository.claimNext() ?: break
                val started = System.nanoTime()
                val context =
                    "job=${item.jobId} item=${item.itemId} attempt=${item.attemptCount + 1}/2"
                log("$context claimed")
                coroutineScope {
                    val work = launch {
                        var stage = JobStage.Ocr
                        try {
                            log("$context stage=ocr begin")
                            val text = ocr.recognize(item.imagePath)
                            log("$context stage=ocr complete chars=${text?.length ?: 0}")
                            stage = JobStage.Infer
                            repository.setStage(item.jobId, JobStage.Infer)
                            log("$context stage=infer begin")
                            val summary =
                                llm.summarize(imageFile(item.imagePath).absolutePath, text)
                            val payload = summaryPayload(summary)
                            log("$context stage=infer complete payloadChars=${payload.length}")
                            stage = JobStage.Saving
                            repository.setStage(item.jobId, JobStage.Saving)
                            log("$context stage=saving begin")
                            repository.complete(item.jobId, text, payload)
                            log("$context completion_returned elapsedMs=${(System.nanoTime() - started) / 1_000_000}")
                        } catch (cancelled: CancellationException) {
                            log("$context cancelled stage=$stage")
                            throw cancelled
                        } catch (error: Exception) {
                            log("$context failed stage=$stage error=${error.javaClass.simpleName}")
                            repository.fail(item.jobId, "analysis_failed")
                            log("$context failure_handled policy=${if (item.attemptCount == 0) "retry_once" else "terminal_failure"}")
                        }
                    }
                    val cancellation = launch {
                        content.observeQueue()
                            .first { queue -> queue.none { it.jobId == item.jobId } }
                        log("$context left_active_queue")
                        work.cancel()
                    }
                    try {
                        work.join()
                    } finally {
                        cancellation.cancelAndJoin()
                    }
                }
            }
        } finally {
            llm.close()
            log("queue model_closed")
        }
        log("queue drain_end")
    }
}

private fun summaryPayload(summary: String): String =
    buildString {
        append("{\"summary\":")
        append('"')
        for (ch in summary) {
            when (ch) {
                '\\' -> append("\\\\")
                '"' -> append("\\\"")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> if (ch.code < 0x20) {
                    append("\\u")
                    append(ch.code.toString(16).padStart(4, '0'))
                } else {
                    append(ch)
                }
            }
        }
        append('"')
        append('}')
    }
