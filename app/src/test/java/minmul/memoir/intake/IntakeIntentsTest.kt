package minmul.memoir.intake

import android.content.Intent
import minmul.memoir.core.model.ItemSource
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class IntakeIntentsTest {
    @Test
    fun `itemSource defaults to share`() {
        assertEquals(ItemSource.Share, IntakeIntents.itemSource(null as String?))
        assertEquals(ItemSource.Share, IntakeIntents.itemSource("unknown"))
    }

    @Test
    fun `itemSource reads picker extra`() {
        assertEquals(ItemSource.Picker, IntakeIntents.itemSource(ItemSource.Picker.storedValue))
        assertEquals(ItemSource.Share, IntakeIntents.itemSource(ItemSource.Share.storedValue))
    }

    @Test
    fun `picker extras for one image are parsed as SEND`() {
        val uris = listOf("content://images/1")

        assertEquals(Intent.ACTION_SEND, IntakeIntents.pickerAction(uris.size))
        assertEquals(
            uris,
            ShareImageParser.parse(
                action = IntakeIntents.pickerAction(uris.size),
                mimeType = "image/*",
                extraStream = uris.single(),
                extraStreams = emptyList(),
                clipDataUris = uris,
            ),
        )
    }

    @Test
    fun `picker extras for multiple images are parsed as SEND_MULTIPLE`() {
        val uris = listOf("content://images/1", "content://images/2")

        assertEquals(Intent.ACTION_SEND_MULTIPLE, IntakeIntents.pickerAction(uris.size))
        assertEquals(
            uris,
            ShareImageParser.parse(
                action = IntakeIntents.pickerAction(uris.size),
                mimeType = "image/*",
                extraStream = null,
                extraStreams = uris,
                clipDataUris = uris,
            ),
        )
    }
}
