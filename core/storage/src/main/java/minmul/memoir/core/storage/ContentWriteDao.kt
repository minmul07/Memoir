package minmul.memoir.core.storage

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Transaction

@Dao
abstract class ContentWriteDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    abstract suspend fun insertItem(entity: ItemEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    abstract suspend fun insertJob(entity: AnalysisJobEntity)

    @Transaction
    open suspend fun insertItemsAndJobs(writes: List<ItemJobWrite>) {
        writes.forEach { write ->
            insertItem(write.item)
            insertJob(write.job)
        }
    }
}

data class ItemJobWrite(
    val item: ItemEntity,
    val job: AnalysisJobEntity,
)
