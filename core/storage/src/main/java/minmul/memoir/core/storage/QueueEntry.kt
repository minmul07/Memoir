package minmul.memoir.core.storage

import minmul.memoir.core.model.JobStatus

data class QueueEntry(
    val jobId: String,
    val itemId: String,
    val filePath: String,
    val status: JobStatus,
)
