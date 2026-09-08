package minmul.memoir.data.model

import kotlinx.coroutines.flow.Flow
import minmul.memoir.core.model.GemmaModel
import java.io.File

enum class GemmaDownloadStatus {
    Pending, Running, Paused, Successful, Failed;

    val isTerminal: Boolean get() = this == Successful || this == Failed
    val isActive: Boolean get() = this == Pending || this == Running || this == Paused
}

data class GemmaDownloadSnapshot(
    val id: Long,
    val status: GemmaDownloadStatus,
    val downloadedBytes: Long = 0,
    val totalBytes: Long? = null,
)

interface GemmaDownloadEngine {
    fun enqueue(model: GemmaModel, destination: File, title: String): Long
    fun query(id: Long): GemmaDownloadSnapshot?
    fun remove(id: Long)
    fun watch(id: Long): Flow<GemmaDownloadSnapshot>
}

interface GemmaDownloadIdStore {
    suspend fun get(model: GemmaModel): Long?
    suspend fun set(model: GemmaModel, id: Long)
    suspend fun clear(model: GemmaModel)
}
