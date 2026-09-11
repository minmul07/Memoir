package minmul.memoir.core.model

import kotlin.math.roundToInt

data class GemmaInferenceSettings(
    val maxOutputToken: Int = DEFAULT_MAX_OUTPUT_TOKEN,
    val topK: Int = DEFAULT_TOP_K,
    val thinkingEnabled: Boolean = DEFAULT_THINKING_ENABLED,
    val topP: Double = DEFAULT_TOP_P,
    val temperature: Double = DEFAULT_TEMPERATURE,
    val speculativeDecodingEnabled: Boolean = DEFAULT_SPECULATIVE_DECODING,
) {
    companion object {
        const val DEFAULT_MAX_OUTPUT_TOKEN = 1024
        const val DEFAULT_TOP_K = 64
        const val DEFAULT_TOP_P = 0.95
        const val DEFAULT_TEMPERATURE = 1.0
        const val DEFAULT_THINKING_ENABLED = false
        const val DEFAULT_SPECULATIVE_DECODING = true
        const val MIN_MAX_OUTPUT_TOKEN = 128
        const val MAX_MAX_OUTPUT_TOKEN = 4096
        const val MAX_OUTPUT_TOKEN_STEP = 128
        const val MIN_TOP_K = 1
        const val MAX_TOP_K = 128
        const val MIN_TOP_P = 0.0
        const val MAX_TOP_P = 1.0
        const val TOP_P_STEP = 0.01
        const val MIN_TEMPERATURE = 0.0
        const val MAX_TEMPERATURE = 2.0
        const val TEMPERATURE_STEP = 0.1

        fun clamp(
            maxOutputToken: Int = DEFAULT_MAX_OUTPUT_TOKEN,
            topK: Int = DEFAULT_TOP_K,
            thinkingEnabled: Boolean = DEFAULT_THINKING_ENABLED,
            topP: Double = DEFAULT_TOP_P,
            temperature: Double = DEFAULT_TEMPERATURE,
            speculativeDecodingEnabled: Boolean = DEFAULT_SPECULATIVE_DECODING,
        ): GemmaInferenceSettings {
            val boundedTokens = maxOutputToken.coerceIn(MIN_MAX_OUTPUT_TOKEN, MAX_MAX_OUTPUT_TOKEN)
            val aligned = MIN_MAX_OUTPUT_TOKEN +
                    ((boundedTokens - MIN_MAX_OUTPUT_TOKEN + MAX_OUTPUT_TOKEN_STEP / 2) /
                            MAX_OUTPUT_TOKEN_STEP) * MAX_OUTPUT_TOKEN_STEP
            return GemmaInferenceSettings(
                maxOutputToken = aligned.coerceIn(MIN_MAX_OUTPUT_TOKEN, MAX_MAX_OUTPUT_TOKEN),
                topK = topK.coerceIn(MIN_TOP_K, MAX_TOP_K),
                thinkingEnabled = thinkingEnabled,
                topP = snap(topP, MIN_TOP_P, MAX_TOP_P, TOP_P_STEP),
                temperature = snap(temperature, MIN_TEMPERATURE, MAX_TEMPERATURE, TEMPERATURE_STEP),
                speculativeDecodingEnabled = speculativeDecodingEnabled,
            )
        }

        private fun snap(value: Double, min: Double, max: Double, step: Double): Double {
            val bounded = value.coerceIn(min, max)
            val aligned = min + ((bounded - min) / step).roundToInt() * step
            val scale = if (step >= 0.1) 10.0 else 100.0
            return ((aligned * scale).roundToInt() / scale).coerceIn(min, max)
        }
    }
}
