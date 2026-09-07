package minmul.memoir.core.storage

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query

@Dao
interface ItemDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: ItemEntity)

    @Query("SELECT * FROM items WHERE id = :id")
    suspend fun getById(id: String): ItemEntity?

    @Query("DELETE FROM items WHERE id = :id")
    suspend fun deleteById(id: String)
}
