package minmul.memoir.core.storage

import androidx.room3.ColumnTypeConverter
import minmul.memoir.core.model.ItemSource
import minmul.memoir.core.model.JobStage
import minmul.memoir.core.model.JobStatus

object MemoirColumnConverters {
    @ColumnTypeConverter
    fun itemSourceToStored(value: ItemSource): String = value.storedValue

    @ColumnTypeConverter
    fun storedToItemSource(value: String): ItemSource = ItemSource.fromStored(value)

    @ColumnTypeConverter
    fun jobStatusToStored(value: JobStatus): String = value.storedValue

    @ColumnTypeConverter
    fun storedToJobStatus(value: String): JobStatus = JobStatus.fromStored(value)

    @ColumnTypeConverter
    fun jobStageToStored(value: JobStage): String = value.storedValue

    @ColumnTypeConverter
    fun storedToJobStage(value: String): JobStage = JobStage.fromStored(value)
}
