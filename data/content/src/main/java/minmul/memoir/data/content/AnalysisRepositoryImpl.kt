package minmul.memoir.data.content

import kotlinx.coroutines.flow.map
import minmul.memoir.core.model.AnalysisPayload
import minmul.memoir.core.model.ItemDetail
import minmul.memoir.core.model.JobStage
import minmul.memoir.core.model.OcrText
import minmul.memoir.core.model.QueueItem
import minmul.memoir.core.storage.DetailEntry
import minmul.memoir.core.storage.MemoirDatabase
import minmul.memoir.core.storage.QueueEntry
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnalysisRepositoryImpl @Inject constructor(
    private val database: MemoirDatabase,
    private val content: ContentRepository,
) : AnalysisRepository {
    private val dao get() = database.analysisWorkDao()
    override fun observeItems() = dao.observeItems().map { entries ->
        entries.map(DetailEntry::toModel)
    }
    override fun observeHistory() = dao.observeHistory().map { it.map(QueueEntry::toModel) }
    override fun observeDetail(itemId: String) = dao.observeDetail(itemId).map { entry ->
        entry?.toModel()
    }
    override suspend fun hasQueuedWork() = dao.hasQueuedWork()
    override suspend fun claimNext() = dao.claimNext(System.currentTimeMillis())?.toModel()
    override suspend fun setStage(jobId: String, stage: JobStage) = dao.setStage(jobId, stage)
    override suspend fun complete(jobId: String, ocrText: String?, payloadJson: String) =
        dao.complete(jobId, OcrText.stored(ocrText), payloadJson, System.currentTimeMillis())
    override suspend fun fail(jobId: String, errorCode: String) =
        dao.fail(jobId, errorCode, System.currentTimeMillis())
    override suspend fun failActiveQueue(errorCode: String) =
        dao.failActiveQueue(errorCode, System.currentTimeMillis())
    override suspend fun recoverInterrupted() = dao.recoverInterrupted(System.currentTimeMillis())
    override suspend fun cancel(jobId: String) = dao.cancel(jobId, System.currentTimeMillis())
    override suspend fun deleteQueue() = dao.deleteQueue()
    override suspend fun deleteItem(itemId: String) {
        // Cancel first so in-flight OCR cannot save while files are removed.
        // Keep the item row until file deletion succeeds, allowing a failed deletion to retry.
        dao.cancelItem(itemId, System.currentTimeMillis())
        content.discardOriginals(listOf(itemId))
        database.itemDao().deleteById(itemId)
    }
    override suspend fun deleteAllItems() {
        // Snapshot IDs so concurrently imported items and their files are not accidentally removed.
        dao.itemIds().forEach { deleteItem(it) }
    }
}

private fun DetailEntry.toModel(): ItemDetail {
    val payload = payloadJson?.let(AnalysisPayload::parse)
    return ItemDetail(
        itemId = itemId,
        imagePath = imagePath,
        status = status,
        ocrText = ocrText,
        createdAt = createdAt,
        title = payload?.title,
        summary = payload?.summary,
        detailedSummary = payload?.detailedSummary,
    )
}

private fun QueueEntry.toModel() =
    QueueItem(jobId, itemId, filePath, status, stage, attemptCount, errorMessage)
