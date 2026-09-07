package minmul.memoir.core.model

/** Order also determines the winner when OCR confidence scores tie. */
enum class OcrModel { Korean, Japanese, Chinese, Latin, Devanagari }

enum class OcrModelStatus { Checking, Missing, Pending, Downloading, Paused, Installing, Ready, Failed }

data class OcrModelState(
    val model: OcrModel,
    val status: OcrModelStatus = OcrModelStatus.Checking,
    val downloadedBytes: Long = 0,
    val totalBytes: Long? = null,
    val enabled: Boolean = true,
) {
    val progress: Float?
        get() = totalBytes?.takeIf { it > 0 }
            ?.let { (downloadedBytes.toDouble() / it).coerceIn(0.0, 1.0).toFloat() }

    val isDownloading: Boolean
        get() = status in setOf(
            OcrModelStatus.Pending, OcrModelStatus.Downloading,
            OcrModelStatus.Paused, OcrModelStatus.Installing,
        )
}
