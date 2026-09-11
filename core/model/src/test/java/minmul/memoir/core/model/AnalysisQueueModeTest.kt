package minmul.memoir.core.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AnalysisQueueModeTest {
    @Test
    fun `only immediate starts analysis automatically`() {
        assertEquals(false, AnalysisQueueMode.Manual.startsAutomatically)
        assertEquals(false, AnalysisQueueMode.Scheduled.startsAutomatically)
        assertEquals(true, AnalysisQueueMode.Immediate.startsAutomatically)
    }
}
