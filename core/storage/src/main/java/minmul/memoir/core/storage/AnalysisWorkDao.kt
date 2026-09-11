package minmul.memoir.core.storage

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Transaction
import kotlinx.coroutines.flow.Flow
import minmul.memoir.core.model.JobStage
import minmul.memoir.core.model.JobStatus

@Dao
abstract class AnalysisWorkDao {
    @Query(
        """
        SELECT i.id AS itemId, i.file_path AS imagePath, j.status,
            r.ocr_text AS ocrText, r.payload_json AS payloadJson,
            i.created_at AS createdAt
        FROM items i
        LEFT JOIN analysis_jobs j ON j.item_id = i.id
        LEFT JOIN analysis_results r ON r.item_id = i.id
        ORDER BY i.created_at DESC, i.id
    """
    )
    abstract fun observeItems(): Flow<List<DetailEntry>>

    @Query(
        """
        SELECT j.id AS jobId, j.item_id AS itemId, i.file_path AS filePath,
            j.status, j.stage, j.attempt_count AS attemptCount, j.error_message AS errorMessage
        FROM analysis_jobs j JOIN items i ON i.id = j.item_id
        WHERE j.status IN ('succeeded', 'failed', 'cancelled')
        ORDER BY j.finished_at DESC, j.id
    """
    )
    abstract fun observeHistory(): Flow<List<QueueEntry>>

    @Query(
        """
        SELECT i.id AS itemId, i.file_path AS imagePath, j.status,
            r.ocr_text AS ocrText, r.payload_json AS payloadJson,
            i.created_at AS createdAt
        FROM items i
        LEFT JOIN analysis_jobs j ON j.item_id = i.id
        LEFT JOIN analysis_results r ON r.item_id = i.id
        WHERE i.id = :itemId
    """
    )
    abstract fun observeDetail(itemId: String): Flow<DetailEntry?>

    @Query("SELECT EXISTS(SELECT 1 FROM analysis_jobs WHERE status = 'queued')")
    abstract suspend fun hasQueuedWork(): Boolean

    @Query(
        """
        SELECT * FROM analysis_jobs
        WHERE status = 'queued'
        ORDER BY attempt_count, queue_order, created_at, id
        LIMIT 1
    """
    )
    protected abstract suspend fun next(): AnalysisJobEntity?

    @Query("SELECT * FROM analysis_jobs WHERE id = :id")
    abstract suspend fun job(id: String): AnalysisJobEntity?

    @Query("SELECT * FROM items WHERE id = :id")
    protected abstract suspend fun item(id: String): ItemEntity?

    @Query(
        """
        UPDATE analysis_jobs
        SET status = 'running', stage = 'ocr', started_at = :now
        WHERE id = :id AND status = 'queued'
    """
    )
    protected abstract suspend fun claim(id: String, now: Long): Int

    @Transaction
    open suspend fun claimNext(now: Long): QueueEntry? {
        val next = next() ?: return null
        if (claim(next.id, now) != 1) return null
        val item = item(next.itemId) ?: return null
        return QueueEntry(
            next.id, next.itemId, item.filePath, JobStatus.Running,
            JobStage.Ocr, next.attemptCount, next.errorMessage
        )
    }

    @Query("UPDATE analysis_jobs SET stage = :stage WHERE id = :id AND status = 'running'")
    abstract suspend fun setStage(id: String, stage: JobStage)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertResult(result: AnalysisResultEntity)

    @Query(
        """
        UPDATE analysis_jobs
        SET status = 'succeeded', error_message = NULL, finished_at = :now
        WHERE id = :id AND status = 'running'
    """
    )
    protected abstract suspend fun succeed(id: String, now: Long)

    @Transaction
    open suspend fun complete(id: String, text: String?, payload: String, now: Long) {
        val job = job(id) ?: return
        if (job.status != JobStatus.Running) return
        insertResult(AnalysisResultEntity(job.itemId, id, job.startedAt ?: now, now, text, payload))
        succeed(id, now)
    }

    @Query(
        """
        UPDATE analysis_jobs SET
            status = CASE WHEN attempt_count = 0 THEN 'queued' ELSE 'failed' END,
            stage = 'waiting',
            finished_at = CASE WHEN attempt_count = 0 THEN NULL ELSE :now END,
            attempt_count = 1, error_message = :error
        WHERE id = :id AND status = 'running'
    """
    )
    abstract suspend fun fail(id: String, error: String, now: Long)

    @Query(
        """
        UPDATE analysis_jobs SET
            status = 'failed',
            stage = 'waiting',
            finished_at = :now,
            error_message = :error
        WHERE status IN ('queued', 'running')
    """
    )
    abstract suspend fun failActiveQueue(error: String, now: Long)

    @Query(
        """
        UPDATE analysis_jobs
        SET status = 'failed', error_message = 'interrupted', finished_at = :now
        WHERE status = 'running'
    """
    )
    abstract suspend fun recoverInterrupted(now: Long)

    @Query(
        """
        UPDATE analysis_jobs
        SET status = 'cancelled', finished_at = :now
        WHERE id = :id AND status IN ('queued', 'running')
    """
    )
    abstract suspend fun cancel(id: String, now: Long)

    @Query(
        """
        UPDATE analysis_jobs
        SET status = 'cancelled', finished_at = :now
        WHERE item_id = :itemId AND status IN ('queued', 'running')
    """
    )
    abstract suspend fun cancelItem(itemId: String, now: Long)

    @Query("DELETE FROM analysis_jobs WHERE status IN ('queued', 'running')")
    abstract suspend fun deleteQueue()

    @Query("SELECT id FROM items")
    abstract suspend fun itemIds(): List<String>
}
