package minmul.memoir.core.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class PreferredReadyGemmaModelTest {
    @Test
    fun `selected ready model is kept`() {
        assertEquals(
            GemmaModel.E4B,
            preferredReadyGemmaModel(GemmaModel.E4B, listOf(GemmaModel.E2B, GemmaModel.E4B)),
        )
    }

    @Test
    fun `missing selected model falls back to smallest ready`() {
        assertEquals(
            GemmaModel.E2B,
            preferredReadyGemmaModel(GemmaModel.E4B, listOf(GemmaModel.E2B)),
        )
    }

    @Test
    fun `unselected ready models pick the smallest file`() {
        assertEquals(
            GemmaModel.E2B,
            preferredReadyGemmaModel(null, listOf(GemmaModel.E4B, GemmaModel.E2B)),
        )
    }

    @Test
    fun `no ready model returns null`() {
        assertNull(preferredReadyGemmaModel(GemmaModel.E2B, emptyList()))
    }
}
