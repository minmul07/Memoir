package minmul.memoir.data.model

import android.app.DownloadManager
import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.core.net.toUri
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import minmul.memoir.core.model.GemmaModel
import java.io.File
import kotlin.time.Duration.Companion.milliseconds

class DownloadManagerGemmaDownloadEngine(
    private val context: Context,
    private val downloadManager: DownloadManager = context.getSystemService(DownloadManager::class.java),
) : GemmaDownloadEngine {
    override fun enqueue(model: GemmaModel, destination: File, title: String): Long {
        destination.parentFile?.mkdirs()
        if (destination.exists()) destination.delete()
        val root = context.getExternalFilesDir(null)
            ?: error("external_files_unavailable")
        val relative = destination.relativeTo(root).invariantSeparatorsPath
        val request = DownloadManager.Request(model.downloadUrl.toUri())
            .setTitle(title)
            .setDescription(model.fileName)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setAllowedOverRoaming(false)
            .setAllowedOverMetered(true)
            .setAllowedNetworkTypes(
                DownloadManager.Request.NETWORK_WIFI or DownloadManager.Request.NETWORK_MOBILE,
            )
            .setDestinationInExternalFilesDir(context, null, relative)
        return downloadManager.enqueue(request)
    }

    override fun query(id: Long): GemmaDownloadSnapshot? {
        val cursor = downloadManager.query(DownloadManager.Query().setFilterById(id)) ?: return null
        cursor.use {
            if (!it.moveToFirst()) return null
            val status = it.getInt(it.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
            val downloaded =
                it.getLong(it.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
            val total =
                it.getLong(it.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
            return GemmaDownloadSnapshot(
                id = id,
                status = status.toGemmaDownloadStatus(),
                downloadedBytes = downloaded.coerceAtLeast(0),
                totalBytes = total.takeIf { bytes -> bytes > 0 },
            )
        }
    }

    override fun remove(id: Long) {
        downloadManager.remove(id)
    }

    override fun watch(id: Long): Flow<GemmaDownloadSnapshot> = callbackFlow {
        fun emitCurrent() {
            val snapshot = query(id)
            if (snapshot == null) {
                close()
                return
            }
            trySend(snapshot)
            if (snapshot.status.isTerminal) close()
        }

        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) = emitCurrent()
            override fun onChange(selfChange: Boolean, uri: Uri?) = emitCurrent()
        }
        context.contentResolver.registerContentObserver(DOWNLOADS_URI, true, observer)
        emitCurrent()
        val poll = launch {
            while (isActive) {
                delay(1_000.milliseconds)
                emitCurrent()
            }
        }
        awaitClose {
            poll.cancel()
            context.contentResolver.unregisterContentObserver(observer)
        }
    }

    private fun Int.toGemmaDownloadStatus(): GemmaDownloadStatus = when (this) {
        DownloadManager.STATUS_PENDING -> GemmaDownloadStatus.Pending
        DownloadManager.STATUS_RUNNING -> GemmaDownloadStatus.Running
        DownloadManager.STATUS_PAUSED -> GemmaDownloadStatus.Paused
        DownloadManager.STATUS_SUCCESSFUL -> GemmaDownloadStatus.Successful
        else -> GemmaDownloadStatus.Failed
    }

    private companion object {
        val DOWNLOADS_URI: Uri = "content://downloads/my_downloads".toUri()
    }
}
