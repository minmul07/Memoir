package minmul.memoir.data.content

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class OriginalImportModeTest {
    @Test
    fun `jpeg mime types copy without reencoding`() {
        assertEquals(OriginalImportMode.CopyJpeg, OriginalImportMode.fromMimeType("image/jpeg"))
        assertEquals(OriginalImportMode.CopyJpeg, OriginalImportMode.fromMimeType("image/jpg"))
        assertEquals(
            OriginalImportMode.CopyJpeg,
            OriginalImportMode.fromMimeType("IMAGE/JPEG; charset=utf-8"),
        )
    }

    @Test
    fun `png webp heif hevc mime types reencode to jpeg`() {
        val mimeTypes = listOf(
            "image/png",
            "image/webp",
            "image/heif",
            "image/heic",
            "image/hevc",
            "video/hevc",
        )

        mimeTypes.forEach { mimeType ->
            assertEquals(
                OriginalImportMode.ReencodeJpeg,
                OriginalImportMode.fromMimeType(mimeType),
                mimeType,
            )
        }
    }

    @Test
    fun `unknown or missing mime types are unsupported`() {
        assertEquals(OriginalImportMode.Unsupported, OriginalImportMode.fromMimeType(null))
        assertEquals(OriginalImportMode.Unsupported, OriginalImportMode.fromMimeType("image/gif"))
        assertEquals(OriginalImportMode.Unsupported, OriginalImportMode.fromMimeType("video/mp4"))
    }
}
