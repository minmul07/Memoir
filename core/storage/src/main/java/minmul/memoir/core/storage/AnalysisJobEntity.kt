package minmul.memoir.core.storage

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey
import minmul.memoir.core.model.JobStage
import minmul.memoir.core.model.JobStatus

@Entity(
    tableName = "analysis_jobs",
    foreignKeys = [
        ForeignKey(
            entity = ItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["item_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["item_id"], unique = true),
    ],
)
data class AnalysisJobEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "item_id")
    val itemId: String,
    val status: JobStatus,
    val stage: JobStage,
    @ColumnInfo(name = "queue_order")
    val queueOrder: Int,
    @ColumnInfo(name = "attempt_count")
    val attemptCount: Int,
    @ColumnInfo(name = "error_message")
    val errorMessage: String?,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    @ColumnInfo(name = "started_at")
    val startedAt: Long?,
    @ColumnInfo(name = "finished_at")
    val finishedAt: Long?,
)
