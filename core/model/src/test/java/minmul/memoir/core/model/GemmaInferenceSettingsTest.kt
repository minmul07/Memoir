package minmul.memoir.core.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class GemmaInferenceSettingsTest {
    @Test
    fun `defaults stay inside the documented range`() {
        val settings = GemmaInferenceSettings()
        assertEquals(1024, settings.maxOutputToken)
        assertEquals(64, settings.topK)
        assertEquals(false, settings.thinkingEnabled)
        assertEquals(0.95, settings.topP)
        assertEquals(1.0, settings.temperature)
        assertEquals(true, settings.speculativeDecodingEnabled)
    }

    @Test
    fun `out of range values are clamped and tokens snap to 128`() {
        assertEquals(
            GemmaInferenceSettings(128, 1, true, 0.0, 0.0, false),
            GemmaInferenceSettings.clamp(0, 0, true, -1.0, -4.0, false),
        )
        assertEquals(
            GemmaInferenceSettings(4096, 128, false, 1.0, 2.0, true),
            GemmaInferenceSettings.clamp(99_999, 500, false, 9.0, 9.0, true),
        )
        assertEquals(
            GemmaInferenceSettings(1024, 40, false, 0.95, 1.0, true),
            GemmaInferenceSettings.clamp(1000, 40, false, 0.947, 0.96),
        )
    }
}
