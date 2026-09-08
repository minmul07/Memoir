package minmul.memoir.feature.queue

import kotlinx.coroutines.flow.flowOf
import minmul.memoir.core.model.ItemSource
import minmul.memoir.core.model.QueueItem
import minmul.memoir.data.content.ContentRepository
import minmul.memoir.data.content.ImportedOriginal

class FakeContentRepository : ContentRepository {
    override fun observeQueue() = flowOf(emptyList<QueueItem>())

    override suspend fun importOriginal(itemId: String, sourceUri: String): ImportedOriginal {
        error("unused")
    }

    override suspend fun enqueueImported(items: List<ImportedOriginal>, source: ItemSource) {
        error("unused")
    }

    override suspend fun discardOriginals(itemIds: List<String>) = Unit

    override fun discardOriginalsAsync(itemIds: List<String>) = Unit
}
