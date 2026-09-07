package minmul.memoir.core.model

data class Item(
    val id: String,
    val createdAt: Long,
    val source: ItemSource,
    val filePath: String,
    val mimeType: String,
)
