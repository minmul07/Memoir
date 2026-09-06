package minmul.memoir.core.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class JobStageTest {
    @Test
    fun `stored values round-trip`() {
        JobStage.entries.forEach { stage ->
            assertEquals(stage, JobStage.fromStored(stage.storedValue))
        }
    }
}
