package minmul.memoir.intake

import android.content.Intent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ShareImageParserTest {
    @Test
    fun `parses a single SEND image`() {
        val result = ShareImageParser.parse(
            action = Intent.ACTION_SEND,
            mimeType = "image/jpeg",
            extraStream = "content://images/1",
            extraStreams = emptyList(),
            clipDataUris = emptyList(),
        )

        assertEquals(listOf("content://images/1"), result)
    }

    @Test
    fun `parses SEND_MULTIPLE images`() {
        val uris = listOf("content://images/1", "content://images/2")

        val result = ShareImageParser.parse(
            action = Intent.ACTION_SEND_MULTIPLE,
            mimeType = "image/*",
            extraStream = null,
            extraStreams = uris,
            clipDataUris = emptyList(),
        )

        assertEquals(uris, result)
    }

    @Test
    fun `uses clip data when SEND extra stream is missing`() {
        val result = ShareImageParser.parse(
            action = Intent.ACTION_SEND,
            mimeType = "image/png",
            extraStream = null,
            extraStreams = emptyList(),
            clipDataUris = listOf("content://images/clip", "content://images/ignored"),
        )

        assertEquals(listOf("content://images/clip"), result)
    }

    @Test
    fun `uses clip data when SEND_MULTIPLE extra streams are missing`() {
        val clipUris = listOf("content://images/a", "content://images/b")

        val result = ShareImageParser.parse(
            action = Intent.ACTION_SEND_MULTIPLE,
            mimeType = "image/jpeg",
            extraStream = null,
            extraStreams = emptyList(),
            clipDataUris = clipUris,
        )

        assertEquals(clipUris, result)
    }

    @Test
    fun `ignores non-image mime types`() {
        val result = ShareImageParser.parse(
            action = Intent.ACTION_SEND,
            mimeType = "text/plain",
            extraStream = "content://images/1",
            extraStreams = emptyList(),
            clipDataUris = emptyList(),
        )

        assertEquals(emptyList<String>(), result)
    }

    @Test
    fun `returns empty when extras are empty`() {
        val result = ShareImageParser.parse(
            action = Intent.ACTION_SEND,
            mimeType = "image/jpeg",
            extraStream = null,
            extraStreams = emptyList(),
            clipDataUris = emptyList(),
        )

        assertEquals(emptyList<String>(), result)
    }
}
