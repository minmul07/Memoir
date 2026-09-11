package minmul.memoir.core.model

data class ItemDetail(
    val itemId: String,
    val imagePath: String,
    val status: JobStatus?,
    val ocrText: String?,
    val createdAt: Long,
    val title: String?,
    val summary: String?,
    val detailedSummary: String?,
)
