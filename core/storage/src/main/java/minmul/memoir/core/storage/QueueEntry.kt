package minmul.memoir.core.storage

import minmul.memoir.core.model.JobStage
import minmul.memoir.core.model.JobStatus

data class QueueEntry(
    val jobId: String,
    val itemId: String,
    val filePath: String,
    val status: JobStatus,
    val stage: JobStage = JobStage.Waiting,
    val attemptCount: Int = 0,
    val errorMessage: String? = null,
)
