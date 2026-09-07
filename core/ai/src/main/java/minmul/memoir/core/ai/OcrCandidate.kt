package minmul.memoir.core.ai

internal data class OcrCandidate(
    val text: String,
    val lineConfidences: List<Float>,
) {
    // Zero is a valid score, including when older Play services cannot provide confidence.
    val averageConfidence: Double
        get() = lineConfidences.filter { it.isFinite() && it in 0f..1f }
            .takeIf { it.isNotEmpty() }?.average() ?: 0.0
}

/** Equal scores retain model order: Korean, Japanese, Chinese, Latin, Devanagari. */
internal fun selectOcrText(candidates: List<OcrCandidate>): String? =
    candidates.filter { it.text.isNotBlank() }.maxByOrNull { it.averageConfidence }?.text