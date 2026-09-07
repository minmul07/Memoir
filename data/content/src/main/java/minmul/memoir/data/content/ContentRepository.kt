package minmul.memoir.data.content

import kotlinx.coroutines.flow.Flow
import minmul.memoir.core.model.QueueItem
import minmul.memoir.core.model.ItemSource

interface ContentRepository {
    fun observeQueue(): Flow<List<QueueItem>>

    suspend fun importOriginal(itemId: String, sourceUri: String): ImportedOriginal

    suspend fun enqueueImported(items: List<ImportedOriginal>, source: ItemSource)

    suspend fun discardOriginals(itemIds: List<String>)

    fun discardOriginalsAsync(itemIds: List<String>)
}
