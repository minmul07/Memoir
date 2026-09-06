package minmul.memoir.intake

import android.content.Intent
import android.net.Uri
import androidx.core.content.IntentCompat

object ShareImageParser {
    fun parse(intent: Intent): List<String> = parse(
        action = intent.action,
        mimeType = intent.type,
        extraStream = IntentCompat.getParcelableExtra(
            intent,
            Intent.EXTRA_STREAM,
            Uri::class.java,
        )?.toString(),
        extraStreams = IntentCompat.getParcelableArrayListExtra(
            intent,
            Intent.EXTRA_STREAM,
            Uri::class.java,
        )?.mapNotNull { it?.toString() }.orEmpty(),
        clipDataUris = buildList {
            val clipData = intent.clipData ?: return@buildList
            for (index in 0 until clipData.itemCount) {
                clipData.getItemAt(index).uri?.toString()?.let(::add)
            }
        },
    )

    fun parse(
        action: String?,
        mimeType: String?,
        extraStream: String?,
        extraStreams: List<String>,
        clipDataUris: List<String>,
    ): List<String> {
        if (!isImageMimeType(mimeType)) {
            return emptyList()
        }
        val uris = when (action) {
            Intent.ACTION_SEND -> {
                listOfNotNull(extraStream).ifEmpty { clipDataUris.take(1) }
            }
            Intent.ACTION_SEND_MULTIPLE -> {
                extraStreams.ifEmpty { clipDataUris }
            }
            else -> emptyList()
        }
        return uris.filter { it.isNotBlank() }.distinct()
    }

    private fun isImageMimeType(mimeType: String?): Boolean {
        if (mimeType == null) {
            return true
        }
        return mimeType.startsWith("image/")
    }
}
