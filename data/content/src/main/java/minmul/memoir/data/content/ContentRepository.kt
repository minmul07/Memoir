package minmul.memoir.data.content

import minmul.memoir.core.model.ItemSource

interface ContentRepository {
    suspend fun importOriginal(itemId: String, sourceUri: String): ImportedOriginal

    suspend fun enqueueImported(items: List<ImportedOriginal>, source: ItemSource)

    suspend fun discardOriginals(itemIds: List<String>)

    fun discardOriginalsAsync(itemIds: List<String>)
}
