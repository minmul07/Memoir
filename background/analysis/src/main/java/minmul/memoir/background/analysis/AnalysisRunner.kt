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
import minmul.memoir.core.ai.OcrEngine
import minmul.memoir.core.model.JobStage
import minmul.memoir.core.model.QueueItem
import minmul.memoir.data.content.AnalysisRepository
import minmul.memoir.data.content.ContentRepository

fun interface FakeAnalysis {
    suspend fun analyze(item: QueueItem, text: String?): String
}

class AnalysisRunner(
    private val repository: AnalysisRepository,
    private val content: ContentRepository,
    private val ocr: OcrEngine,
    private val analysis: FakeAnalysis,
    private val log: (String) -> Unit = {},
) {
    private val mutex = Mutex()

    suspend fun drain() = mutex.withLock {
        log("queue drain_start recovery_begin")
        repository.recoverInterrupted()
        log("queue recovery_complete")
        while (currentCoroutineContext().isActive) {
            val item = repository.claimNext() ?: break
            val started = System.nanoTime()
            val context = "job=${item.jobId} item=${item.itemId} attempt=${item.attemptCount + 1}/2"
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
                        val payload = analysis.analyze(item, text)
                        log("$context stage=infer complete payloadChars=${payload.length}")
                        stage = JobStage.Saving
                        repository.setStage(item.jobId, JobStage.Saving)
                        log("$context stage=saving begin")
                        repository.complete(item.jobId, text, payload)
                        // Cancelled/deleted jobs may be ignored by the repository.
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
                    content.observeQueue().first { queue -> queue.none { it.jobId == item.jobId } }
                    // Success also removes a job from the active queue.
                    log("$context left_active_queue")
                    work.cancel()
                }
                try { work.join() } finally { cancellation.cancelAndJoin() }
            }
        }
        log("queue drain_end")
    }
}
