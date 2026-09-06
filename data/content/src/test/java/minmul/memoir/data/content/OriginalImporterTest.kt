package minmul.memoir.data.content

import java.io.File
import java.io.IOException
import minmul.memoir.core.storage.OriginalFileStore
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class OriginalImporterTest {
    @TempDir
    lateinit var filesDir: File

    @Test
    fun `jpeg is copied without encoding`() {
        val encoder = RecordingEncoder()
        val importer = importer(
            mimeType = "image/jpeg",
            bytes = "jpeg-bytes".toByteArray(),
            encoder = encoder,
        )

        val imported = importer.import("item-1", "content://images/1")

        assertEquals(
            ImportedOriginal("item-1", "items/item-1/original", "image/jpeg"),
            imported,
        )
        assertEquals("jpeg-bytes", File(filesDir, imported.filePath).readText())
        assertFalse(encoder.called)
    }

    @Test
    fun `png is reencoded to jpeg bytes`() {
        val importer = importer(
            mimeType = "image/png",
            bytes = "png-bytes".toByteArray(),
            encoder = ImageJpegEncoder { _, output -> output.write("jpeg-80".toByteArray()) },
        )

        val imported = importer.import("item-1", "content://images/1")

        assertEquals("image/jpeg", imported.mimeType)
        assertEquals("jpeg-80", File(filesDir, imported.filePath).readText())
    }

    @Test
    fun `unsupported mime type does not leave files`() {
        val importer = importer(
            mimeType = "image/gif",
            bytes = "gif".toByteArray(),
            encoder = RecordingEncoder(),
        )

        val error = runCatching { importer.import("item-1", "content://images/1") }.exceptionOrNull()

        assertTrue(error is IllegalStateException)
        assertFalse(File(filesDir, "items/item-1").exists())
    }

    @Test
    fun `encoder failure deletes the item directory`() {
        val importer = importer(
            mimeType = "image/png",
            bytes = "png".toByteArray(),
            encoder = ImageJpegEncoder { _, _ -> throw IOException("encode failed") },
        )

        val error = runCatching { importer.import("item-1", "content://images/1") }.exceptionOrNull()

        assertTrue(error is IOException)
        assertFalse(File(filesDir, "items/item-1").exists())
    }

    private fun importer(
        mimeType: String?,
        bytes: ByteArray,
        encoder: ImageJpegEncoder,
    ): OriginalImporter = OriginalImporter(
        fileStore = OriginalFileStore(filesDir),
        resolveMimeType = { mimeType },
        openStream = { bytes.inputStream() },
        jpegEncoder = encoder,
    )

    private class RecordingEncoder : ImageJpegEncoder {
        var called: Boolean = false

        override fun encode(input: java.io.InputStream, output: java.io.OutputStream) {
            called = true
        }
    }
}
