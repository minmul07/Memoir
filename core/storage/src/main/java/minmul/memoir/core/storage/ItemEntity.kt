package minmul.memoir.core.storage

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.PrimaryKey
import minmul.memoir.core.model.ItemSource

@Entity(tableName = "items")
data class ItemEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    val source: ItemSource,
    @ColumnInfo(name = "file_path")
    val filePath: String,
    @ColumnInfo(name = "mime_type")
    val mimeType: String,
)
