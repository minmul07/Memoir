package minmul.memoir.data.content

import android.content.Context
import android.util.Log
import androidx.core.net.toUri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import minmul.memoir.core.model.ItemSource
import minmul.memoir.core.model.JobStage
import minmul.memoir.core.model.JobStatus
import minmul.memoir.core.model.QueueItem
import minmul.memoir.core.storage.AnalysisJobEntity
import minmul.memoir.core.storage.ItemEntity
import minmul.memoir.core.storage.ItemJobWrite
import minmul.memoir.core.storage.MemoirDatabase
import minmul.memoir.core.storage.OriginalFileStore
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContentRepositoryImpl @Inject constructor(
    @ApplicationContext context: Context,
    private val database: MemoirDatabase,
    jpegEncoder: AndroidImageJpegEncoder,
    @ImportDispatcher private val importDispatcher: CoroutineDispatcher,
) : ContentRepository {
    private val originalFileStore = OriginalFileStore(context.filesDir)
    private val importer = OriginalImporter(
        fileStore = originalFileStore,
        resolveMimeType = { sourceUri ->
            context.contentResolver.getType(sourceUri.toUri())
        },
        openStream = { sourceUri ->
            context.contentResolver.openInputStream(sourceUri.toUri())
                ?: error("cannot open $sourceUri")
        },
        jpegEncoder = jpegEncoder,
    )
    private val backgroundScope = CoroutineScope(SupervisorJob() + importDispatcher)

    override fun observeQueue() = database.analysisJobDao().observeQueue().map { entries ->
        entries.map { entry ->
            QueueItem(
                jobId = entry.jobId,
                itemId = entry.itemId,
                imagePath = entry.filePath,
                status = entry.status,
                stage = entry.stage,
                attemptCount = entry.attemptCount,
                errorMessage = entry.errorMessage,
            )
        }
    }

    override suspend fun importOriginal(itemId: String, sourceUri: String): ImportedOriginal =
        withContext(importDispatcher) {
            importer.import(itemId, sourceUri)
        }

    override suspend fun enqueueImported(items: List<ImportedOriginal>, source: ItemSource) {
        if (items.isEmpty()) {
            return
        }
        val now = System.currentTimeMillis()
        val writes = items.map { imported ->
            ItemJobWrite(
                item = ItemEntity(
                    id = imported.itemId,
                    createdAt = now,
                    source = source,
                    filePath = imported.filePath,
                    mimeType = imported.mimeType,
                ),
                job = AnalysisJobEntity(
                    id = UUID.randomUUID().toString(),
                    itemId = imported.itemId,
                    status = JobStatus.Queued,
                    stage = JobStage.Waiting,
                    queueOrder = 0,
                    attemptCount = 0,
                    errorMessage = null,
                    createdAt = now,
                    startedAt = null,
                    finishedAt = null,
                ),
            )
        }
        database.contentWriteDao().insertItemsAndJobs(writes)
    }

    override suspend fun discardOriginals(itemIds: List<String>) {
        if (itemIds.isEmpty()) {
            return
        }
        withContext(importDispatcher) {
            itemIds.forEach(originalFileStore::deleteItemDir)
        }
    }

    override fun discardOriginalsAsync(itemIds: List<String>) {
        if (itemIds.isEmpty()) {
            return
        }
        backgroundScope.launch {
            try {
                discardOriginals(itemIds)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                // Intake cancellation cleanup must not crash the application.
                // Explicit item deletion uses the suspending API and reports failure to the UI.
                Log.w("Memoir", "Could not discard temporary originals")
            }
        }
    }
}
