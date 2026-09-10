package minmul.memoir.feature.intake

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.flowOf
import minmul.memoir.core.model.ItemSource
import minmul.memoir.core.model.QueueItem
import minmul.memoir.data.content.ContentRepository
import minmul.memoir.data.content.ImportedOriginal

class FakeContentRepository(
    failUris: Set<String> = emptySet(),
    holdUris: Set<String> = emptySet(),
    private var enqueueFailuresRemaining: Int = 0,
) : ContentRepository {
    override fun observeQueue() = flowOf(emptyList<QueueItem>())

    private val failUris = failUris
    private val holds = holdUris.associateWith { CompletableDeferred<Unit>() }
    val imported = mutableListOf<Pair<String, String>>()
    val enqueued = mutableListOf<List<ImportedOriginal>>()
    val enqueueSources = mutableListOf<ItemSource>()
    val discarded = mutableListOf<List<String>>()
    var enqueueAttempts = 0
        private set

    fun release(uri: String) {
        holds.getValue(uri).complete(Unit)
    }

    override suspend fun importOriginal(itemId: String, sourceUri: String): ImportedOriginal {
        imported += itemId to sourceUri
        holds[sourceUri]?.await()
        if (sourceUri in failUris) {
            error("import failed")
        }
        return ImportedOriginal(
            itemId = itemId,
            filePath = "items/$itemId/original",
            mimeType = "image/jpeg",
        )
    }

    override suspend fun enqueueImported(items: List<ImportedOriginal>, source: ItemSource) {
        enqueueAttempts++
        if (enqueueFailuresRemaining > 0) {
            enqueueFailuresRemaining--
            error("enqueue failed")
        }
        enqueueSources += source
        enqueued += items
    }

    override suspend fun discardOriginals(itemIds: List<String>) {
        discarded += itemIds
    }

    override fun discardOriginalsAsync(itemIds: List<String>) {
        discarded += itemIds
    }
}
