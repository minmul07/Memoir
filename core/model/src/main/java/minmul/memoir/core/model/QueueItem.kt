package minmul.memoir.core.model

data class QueueItem(
    val jobId: String,
    val itemId: String,
    val imagePath: String,
    val status: JobStatus,
)
