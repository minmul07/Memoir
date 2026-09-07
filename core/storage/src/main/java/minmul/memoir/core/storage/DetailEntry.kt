package minmul.memoir.core.storage

import minmul.memoir.core.model.JobStatus

data class DetailEntry(
    val itemId: String,
    val imagePath: String,
    val status: JobStatus?,
    val ocrText: String?,
    val payloadJson: String?,
)
