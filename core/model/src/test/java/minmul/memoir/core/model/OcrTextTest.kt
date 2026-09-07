package minmul.memoir.core.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class OcrTextTest {
    @Test
    fun `blank ocr text is stored as null`() {
        assertNull(OcrText.stored(null))
        assertNull(OcrText.stored(""))
        assertNull(OcrText.stored("  "))
        assertEquals("hello", OcrText.stored("hello"))
    }
}
