package minmul.memoir.core.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class GemmaModelTest {
    @Test
    fun `progress stays indeterminate for unknown size and is bounded when size is known`() {
        assertNull(GemmaModelState(GemmaModel.E2B, totalBytes = null).progress)
        assertNull(GemmaModelState(GemmaModel.E2B, totalBytes = 0).progress)
        assertEquals(
            1f,
            GemmaModelState(GemmaModel.E2B, downloadedBytes = 200, totalBytes = 100).progress,
        )
        assertEquals(
            0f,
            GemmaModelState(GemmaModel.E2B, downloadedBytes = -10, totalBytes = 100).progress,
        )
    }

    @Test
    fun `download urls point at the public litertlm artifacts`() {
        assertEquals(
            "https://huggingface.co/litert-community/gemma-4-E4B-it-litert-lm/resolve/main/gemma-4-E4B-it.litertlm",
            GemmaModel.E4B.downloadUrl,
        )
        assertEquals(
            "https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm/resolve/main/gemma-4-E2B-it.litertlm",
            GemmaModel.E2B.downloadUrl,
        )
        assertEquals(3_288_600_000L, GemmaModel.E4B.minBytes)
        assertEquals(2_324_700_000L, GemmaModel.E2B.minBytes)
    }
}
