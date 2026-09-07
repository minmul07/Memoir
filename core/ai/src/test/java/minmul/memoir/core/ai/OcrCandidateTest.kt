package minmul.memoir.core.ai

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class OcrCandidateTest {
    @Test
    fun `selects highest mean rather than highest individual confidence or sum`() {
        val candidates = listOf(
            OcrCandidate("한국어", listOf(1f, 0.2f)),
            OcrCandidate("日本語", listOf(0.9f, 0.9f)),
            OcrCandidate("中文", listOf(0.8f, 0.8f, 0.8f)),
            OcrCandidate("English", listOf(0.7f)),
            OcrCandidate("हिन्दी", listOf(0.5f)),
        )
        assertEquals("日本語", selectOcrText(candidates))
    }

    @Test
    fun `any of the five models can win`() {
        repeat(5) { winner ->
            val candidates = List(5) { index ->
                OcrCandidate("model $index", listOf(if (index == winner) 0.9f else 0.5f))
            }
            assertEquals("model $winner", selectOcrText(candidates))
        }
    }

    @Test
    fun `blank results are excluded and all blank returns null`() {
        assertEquals(
            "text", selectOcrText(
                listOf(
                    OcrCandidate("  ", listOf(1f)),
                    OcrCandidate("text", listOf(0f)),
                )
            )
        )
        assertNull(selectOcrText(listOf(OcrCandidate("", emptyList()))))
        assertNull(selectOcrText(emptyList()))
    }

    @Test
    fun `ties and unavailable confidence preserve model order`() {
        for (scores in listOf(emptyList(), listOf(0f), listOf(0.8f))) {
            assertEquals(
                "first", selectOcrText(
                    listOf(
                        OcrCandidate("first", scores), OcrCandidate("second", scores),
                    )
                )
            )
        }
    }

    @Test
    fun `invalid confidence cannot outrank valid results`() {
        assertEquals(
            "valid", selectOcrText(
                listOf(
                    OcrCandidate("invalid", listOf(Float.NaN, Float.POSITIVE_INFINITY, -1f, 2f)),
                    OcrCandidate("valid", listOf(0.5f)),
                )
            )
        )
    }
}