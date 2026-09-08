package minmul.memoir.feature.queue

import kotlinx.coroutines.flow.flowOf
import minmul.memoir.core.model.ItemDetail
import minmul.memoir.core.model.JobStage
import minmul.memoir.core.model.QueueItem
import minmul.memoir.data.content.AnalysisRepository

class FakeAnalysisRepository : AnalysisRepository {
    val cancelled = mutableListOf<String>()
    var cancelFails = false

    override fun observeItems() = flowOf(emptyList<ItemDetail>())
    override fun observeHistory() = flowOf(emptyList<QueueItem>())
    override fun observeDetail(itemId: String) = flowOf<ItemDetail?>(null)
    override suspend fun claimNext(): QueueItem? = null
    override suspend fun setStage(jobId: String, stage: JobStage) = Unit
    override suspend fun complete(jobId: String, ocrText: String?, payloadJson: String) = Unit
    override suspend fun fail(jobId: String, errorCode: String) = Unit
    override suspend fun recoverInterrupted() = Unit

    override suspend fun cancel(jobId: String) {
        check(!cancelFails) { "cancel_failed" }
        cancelled += jobId
    }

    override suspend fun deleteItem(itemId: String) = Unit
    override suspend fun deleteAllItems() = Unit
    override suspend fun deleteQueue() = Unit
}
