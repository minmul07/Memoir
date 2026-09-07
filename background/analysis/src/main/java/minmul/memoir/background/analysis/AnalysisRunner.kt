package minmul.memoir.background.analysis

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
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
) {
    private val mutex = Mutex()

    suspend fun drain() = mutex.withLock {
        repository.recoverInterrupted()
        while (currentCoroutineContext().isActive) {
            val item = repository.claimNext() ?: break
            coroutineScope {
                val work = launch {
                    try {
                        val text = ocr.recognize(item.imagePath)
                        repository.setStage(item.jobId, JobStage.Infer)
                        val payload = analysis.analyze(item, text)
                        repository.setStage(item.jobId, JobStage.Saving)
                        repository.complete(item.jobId, text, payload)
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (_: Exception) {
                        repository.fail(item.jobId, "analysis_failed")
                    }
                }
                val cancellation = launch {
                    content.observeQueue().first { queue -> queue.none { it.jobId == item.jobId } }
                    work.cancel()
                }
                try { work.join() } finally { cancellation.cancelAndJoin() }
            }
        }
    }
}
