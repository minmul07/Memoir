package minmul.memoir.core.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ItemFilesTest {
    @Test
    fun `original relative path is items id original`() {
        assertEquals("items/abc/original", ItemFiles.originalRelativePath("abc"))
    }
}
