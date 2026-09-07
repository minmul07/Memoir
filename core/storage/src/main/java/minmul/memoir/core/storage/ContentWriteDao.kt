package minmul.memoir.core.storage

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Transaction

@Dao
abstract class ContentWriteDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    abstract suspend fun insertItem(entity: ItemEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    abstract suspend fun insertJob(entity: AnalysisJobEntity)

    @Query("SELECT MAX(queue_order) FROM analysis_jobs")
    protected abstract suspend fun maxQueueOrder(): Int?

    @Transaction
    open suspend fun insertItemsAndJobs(writes: List<ItemJobWrite>) {
        if (writes.isEmpty()) {
            return
        }
        val base = maxQueueOrder() ?: -1
        writes.forEachIndexed { index, write ->
            insertItem(write.item)
            insertJob(write.job.copy(queueOrder = base + 1 + index))
        }
    }
}

data class ItemJobWrite(
    val item: ItemEntity,
    val job: AnalysisJobEntity,
)
