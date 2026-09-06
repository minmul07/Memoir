package minmul.memoir.core.model

data class Analysis(
    val itemId: String,
    val jobId: String,
    val startedAt: Long,
    val completedAt: Long,
    val ocrText: String?,
    val payloadJson: String,
)
