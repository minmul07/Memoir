package minmul.memoir.core.model

data class ItemDetail(
    val itemId: String,
    val imagePath: String,
    val status: JobStatus?,
    val ocrText: String?,
    val createdAt: Long,
    val title: String?,
    val detailedSummary: String?,
    val time: List<AnalysisEntity> = emptyList(),
    val period: List<AnalysisEntity> = emptyList(),
    val location: List<AnalysisEntity> = emptyList(),
    val account: List<AnalysisEntity> = emptyList(),
    val phone: List<AnalysisEntity> = emptyList(),
)
