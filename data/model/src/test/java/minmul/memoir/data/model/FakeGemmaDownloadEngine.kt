package minmul.memoir.data.model

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.update
import minmul.memoir.core.model.GemmaModel
import java.io.File

internal class FakeGemmaDownloadEngine : GemmaDownloadEngine {
    private var nextId = 1L
    val enqueued = mutableListOf<GemmaModel>()
    val removed = mutableListOf<Long>()
    val destinations = mutableMapOf<Long, File>()
    private val snapshots = MutableStateFlow<Map<Long, GemmaDownloadSnapshot>>(emptyMap())

    override fun enqueue(model: GemmaModel, destination: File, title: String): Long {
        val id = nextId++
        enqueued += model
        destinations[id] = destination
        snapshots.update {
            it + (id to GemmaDownloadSnapshot(id, GemmaDownloadStatus.Pending))
        }
        return id
    }

    override fun query(id: Long): GemmaDownloadSnapshot? = snapshots.value[id]

    override fun remove(id: Long) {
        removed += id
        destinations[id]?.delete()
        snapshots.update { it - id }
    }

    override fun watch(id: Long): Flow<GemmaDownloadSnapshot> =
        snapshots.mapNotNull { it[id] }.distinctUntilChanged()

    fun set(
        id: Long,
        status: GemmaDownloadStatus,
        downloadedBytes: Long = 0,
        totalBytes: Long? = null,
    ) {
        snapshots.update {
            it + (id to GemmaDownloadSnapshot(id, status, downloadedBytes, totalBytes))
        }
    }

    fun succeed(id: Long, bytes: Int = 32) {
        val destination = destinations.getValue(id)
        destination.parentFile?.mkdirs()
        destination.writeBytes(ByteArray(bytes))
        set(id, GemmaDownloadStatus.Successful, bytes.toLong(), bytes.toLong())
    }
}

internal class InMemoryGemmaDownloadIdStore : GemmaDownloadIdStore {
    private val ids = mutableMapOf<GemmaModel, Long>()

    override suspend fun get(model: GemmaModel): Long? = ids[model]

    override suspend fun set(model: GemmaModel, id: Long) {
        ids[model] = id
    }

    override suspend fun clear(model: GemmaModel) {
        ids.remove(model)
    }
}
