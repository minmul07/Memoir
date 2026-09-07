package minmul.memoir.data.content

import kotlinx.coroutines.flow.Flow
import minmul.memoir.core.model.*

interface AnalysisRepository {
    fun observeItems(): Flow<List<ItemDetail>>
    fun observeHistory(): Flow<List<QueueItem>>
    fun observeDetail(itemId: String): Flow<ItemDetail?>
    suspend fun claimNext(): QueueItem?
    suspend fun setStage(jobId: String, stage: JobStage)
    suspend fun complete(jobId: String, ocrText: String?, payloadJson: String)
    suspend fun fail(jobId: String, errorCode: String)
    suspend fun recoverInterrupted()
    suspend fun cancel(jobId: String)
    suspend fun deleteItem(itemId: String)
    suspend fun deleteAllItems()
    suspend fun deleteQueue()
}
