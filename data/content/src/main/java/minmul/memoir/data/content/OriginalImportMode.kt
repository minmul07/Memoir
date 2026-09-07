package minmul.memoir.data.content

enum class OriginalImportMode {
    CopyJpeg,
    ReencodeJpeg,
    Unsupported,
    ;

    companion object {
        fun fromMimeType(mimeType: String?): OriginalImportMode {
            val normalized = mimeType
                ?.substringBefore(';')
                ?.trim()
                ?.lowercase()
                .orEmpty()
            return when (normalized) {
                "image/jpeg", "image/jpg" -> CopyJpeg
                "image/png",
                "image/webp",
                "image/heif",
                "image/heic",
                "image/hevc",
                "video/hevc",
                -> ReencodeJpeg
                else -> Unsupported
            }
        }
    }
}
