package minmul.memoir.core.model

data class ItemDetail(
    val itemId: String,
    val imagePath: String,
    val status: JobStatus?,
    val ocrText: String?,
    val payloadJson: String?,
)
