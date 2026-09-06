package minmul.memoir.core.storage

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query

@Dao
interface AnalysisJobDao {
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
