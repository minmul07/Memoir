package minmul.memoir.feature.main

import kotlinx.coroutines.flow.flowOf
import minmul.memoir.core.model.ItemDetail
import minmul.memoir.core.model.JobStage
import minmul.memoir.core.model.QueueItem
import minmul.memoir.data.content.AnalysisRepository

class FakeAnalysisRepository : AnalysisRepository {
    val deleted = mutableListOf<String>()
    var deleteFails = false

    override fun observeItems() = flowOf(emptyList<ItemDetail>())
    override fun observeHistory() = flowOf(emptyList<QueueItem>())
    override fun observeDetail(itemId: String) = flowOf<ItemDetail?>(null)
    override suspend fun hasQueuedWork() = false
    override suspend fun claimNext(): QueueItem? = null
    override suspend fun setStage(jobId: String, stage: JobStage) = Unit
    override suspend fun complete(jobId: String, ocrText: String?, payloadJson: String) = Unit
    override suspend fun fail(jobId: String, errorCode: String) = Unit
    override suspend fun failActiveQueue(errorCode: String) = Unit
    override suspend fun recoverInterrupted() = Unit
    override suspend fun cancel(jobId: String) = Unit

    override suspend fun deleteItem(itemId: String) {
        check(!deleteFails) { "delete_failed" }
        deleted += itemId
    }

    override suspend fun deleteAllItems() = Unit
    override suspend fun deleteQueue() = Unit
}
