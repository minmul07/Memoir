package minmul.memoir.core.storage

import kotlinx.coroutines.flow.Flow
import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query

@Dao
interface AnalysisJobDao {
    @Query("""
        SELECT j.id AS jobId, j.item_id AS itemId, i.file_path AS filePath, j.status
        FROM analysis_jobs j INNER JOIN items i ON i.id = j.item_id
        WHERE j.status IN ('queued', 'running')
        ORDER BY j.queue_order ASC, j.created_at ASC, j.id ASC
    """)
    fun observeQueue(): Flow<List<QueueEntry>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: AnalysisJobEntity)

    @Query("SELECT * FROM analysis_jobs WHERE id = :id")
    suspend fun getById(id: String): AnalysisJobEntity?

    @Query("SELECT * FROM analysis_jobs WHERE item_id = :itemId")
    suspend fun getByItemId(itemId: String): AnalysisJobEntity?

    @Query("SELECT MAX(queue_order) FROM analysis_jobs")
    suspend fun maxQueueOrder(): Int?

    @Query("DELETE FROM analysis_jobs WHERE id = :id")
    suspend fun deleteById(id: String)
}
