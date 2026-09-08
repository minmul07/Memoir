package minmul.memoir.core.model

enum class GemmaModel(
    val fileName: String,
    val hubRepo: String,
    val expectedBytes: Long,
) {
    E4B(
        fileName = "gemma-4-E4B-it.litertlm",
        hubRepo = "litert-community/gemma-4-E4B-it-litert-lm",
        expectedBytes = 3_654_000_000L,
    ),
    E2B(
        fileName = "gemma-4-E2B-it.litertlm",
        hubRepo = "litert-community/gemma-4-E2B-it-litert-lm",
        expectedBytes = 2_583_000_000L,
    ),
    ;

    val minBytes: Long get() = expectedBytes * 9 / 10

    val downloadUrl: String
        get() = "https://huggingface.co/$hubRepo/resolve/main/$fileName"
}

enum class GemmaModelStatus { Checking, Missing, Pending, Downloading, Paused, Ready, Failed }

data class GemmaModelState(
    val model: GemmaModel,
    val status: GemmaModelStatus = GemmaModelStatus.Checking,
    val downloadedBytes: Long = 0,
    val totalBytes: Long? = null,
    val selected: Boolean = false,
) {
    val progress: Float?
        get() = totalBytes?.takeIf { it > 0 }
            ?.let { (downloadedBytes.toDouble() / it).coerceIn(0.0, 1.0).toFloat() }

    val isDownloading: Boolean
        get() = status in setOf(
            GemmaModelStatus.Pending,
            GemmaModelStatus.Downloading,
            GemmaModelStatus.Paused,
        )
}
