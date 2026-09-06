package minmul.memoir.core.storage

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.PrimaryKey

@Entity(
    tableName = "analysis_results",
    foreignKeys = [
        ForeignKey(
            entity = ItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["item_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class AnalysisResultEntity(
    @PrimaryKey
    @ColumnInfo(name = "item_id")
    val itemId: String,
    @ColumnInfo(name = "job_id")
    val jobId: String,
    @ColumnInfo(name = "started_at")
    val startedAt: Long,
    @ColumnInfo(name = "completed_at")
    val completedAt: Long,
    @ColumnInfo(name = "ocr_text")
    val ocrText: String?,
    @ColumnInfo(name = "payload_json")
    val payloadJson: String,
)
