package minmul.memoir.data.content

import java.io.InputStream
import minmul.memoir.core.storage.OriginalFileStore

class OriginalImporter(
    private val fileStore: OriginalFileStore,
    private val resolveMimeType: (sourceUri: String) -> String?,
    private val openStream: (sourceUri: String) -> InputStream,
    private val jpegEncoder: ImageJpegEncoder,
) {
    fun import(itemId: String, sourceUri: String): ImportedOriginal {
        try {
            val mimeType = resolveMimeType(sourceUri)
            val filePath = when (OriginalImportMode.fromMimeType(mimeType)) {
                OriginalImportMode.CopyJpeg -> copy(itemId, sourceUri)
                OriginalImportMode.ReencodeJpeg -> reencode(itemId, sourceUri)
                OriginalImportMode.Unsupported -> error("unsupported mime type: $mimeType")
            }
            return ImportedOriginal(
                itemId = itemId,
                filePath = filePath,
                mimeType = JPEG_MIME_TYPE,
            )
        } catch (error: Throwable) {
            fileStore.deleteItemDir(itemId)
            throw error
        }
    }

    private fun copy(itemId: String, sourceUri: String): String =
        openStream(sourceUri).use { input ->
            fileStore.writeAtomically(itemId) { output -> input.copyTo(output) }
        }

    private fun reencode(itemId: String, sourceUri: String): String =
        openStream(sourceUri).use { input ->
            fileStore.writeAtomically(itemId) { output -> jpegEncoder.encode(input, output) }
        }

    private companion object {
        const val JPEG_MIME_TYPE = "image/jpeg"
    }
}
