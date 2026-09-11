package minmul.memoir.background.analysis

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import minmul.memoir.core.model.ItemDetail
import minmul.memoir.core.model.ItemSource
import minmul.memoir.core.model.JobStage
import minmul.memoir.core.model.JobStatus
import minmul.memoir.core.model.QueueItem
import minmul.memoir.data.content.AnalysisRepository
import minmul.memoir.data.content.ContentRepository
import minmul.memoir.data.content.ImportedOriginal

class FakeAnalysisRepository(ids: List<String>) : AnalysisRepository, ContentRepository {
    val jobs = MutableStateFlow(ids.map { QueueItem(it, it, it, JobStatus.Queued) })
    val completed = mutableListOf<String>()
    val payloads = mutableListOf<String>()
    override fun observeQueue() = jobs.map { rows ->
        rows.filter {
            it.status in listOf(
                JobStatus.Queued,
                JobStatus.Running
            )
        }
    }

    override fun observeItems(): Flow<List<ItemDetail>> = flowOf(emptyList())
    override fun observeHistory() = jobs.map { rows ->
        rows.filter {
            it.status in listOf(
                JobStatus.Succeeded,
                JobStatus.Failed,
                JobStatus.Cancelled
            )
        }
    }

    override fun observeDetail(itemId: String): Flow<ItemDetail?> = flowOf(null)
    override suspend fun hasQueuedWork() = jobs.value.any { it.status == JobStatus.Queued }
    private fun update(id: String, transform: (QueueItem) -> QueueItem) {
        jobs.value = jobs.value.map { if (it.jobId == id) transform(it) else it }
    }

    override suspend fun claimNext(): QueueItem? {
        val next =
            jobs.value.filter { it.status == JobStatus.Queued }.minByOrNull { it.attemptCount }
                ?: return null
        val running = next.copy(status = JobStatus.Running)
        update(next.jobId) { running }
        return running
    }

    override suspend fun setStage(jobId: String, stage: JobStage) {
        update(jobId) { it.copy(stage = stage) }
    }

    override suspend fun complete(jobId: String, ocrText: String?, payloadJson: String) {
        update(jobId) {
            if (it.status == JobStatus.Running) {
                completed += jobId
                payloads += payloadJson
                it.copy(status = JobStatus.Succeeded)
            } else it
        }
    }

    override suspend fun fail(jobId: String, errorCode: String) {
        update(jobId) {
            it.copy(
                status = if (it.attemptCount == 0) JobStatus.Queued else JobStatus.Failed,
                attemptCount = 1,
                errorMessage = errorCode
            )
        }
    }

    override suspend fun failActiveQueue(errorCode: String) {
        jobs.value = jobs.value.map {
            if (it.status == JobStatus.Queued || it.status == JobStatus.Running) {
                it.copy(status = JobStatus.Failed, errorMessage = errorCode)
            } else it
        }
    }

    override suspend fun recoverInterrupted() {
        jobs.value =
            jobs.value.map { if (it.status == JobStatus.Running) it.copy(status = JobStatus.Failed) else it }
    }

    override suspend fun cancel(jobId: String) {
        update(jobId) { it.copy(status = JobStatus.Cancelled) }
    }

    override suspend fun deleteItem(itemId: String) {
        jobs.value = jobs.value.filterNot { it.itemId == itemId }
    }

    override suspend fun deleteAllItems() {
        jobs.value = emptyList()
    }

    override suspend fun deleteQueue() {
        jobs.value = jobs.value.filter { it.status !in listOf(JobStatus.Queued, JobStatus.Running) }
    }

    override suspend fun importOriginal(itemId: String, sourceUri: String): ImportedOriginal =
        error("unused")

    override suspend fun enqueueImported(items: List<ImportedOriginal>, source: ItemSource) = Unit
    override suspend fun discardOriginals(itemIds: List<String>) = Unit
    override fun discardOriginalsAsync(itemIds: List<String>) = Unit
}
