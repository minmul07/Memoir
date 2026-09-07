package minmul.memoir.core.model

data class AnalysisJob(
    val id: String,
    val itemId: String,
    val status: JobStatus,
    val stage: JobStage,
    val queueOrder: Int,
    val attemptCount: Int,
    val errorMessage: String?,
    val createdAt: Long,
    val startedAt: Long?,
    val finishedAt: Long?,
)
