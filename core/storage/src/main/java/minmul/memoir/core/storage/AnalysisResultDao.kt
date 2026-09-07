package minmul.memoir.core.storage

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query

@Dao
interface AnalysisResultDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: AnalysisResultEntity)

    @Query("SELECT * FROM analysis_results WHERE item_id = :itemId")
    suspend fun getByItemId(itemId: String): AnalysisResultEntity?

    @Query("DELETE FROM analysis_results WHERE item_id = :itemId")
    suspend fun deleteByItemId(itemId: String)
}
