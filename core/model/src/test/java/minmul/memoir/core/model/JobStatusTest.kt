package minmul.memoir.core.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class JobStatusTest {
    @Test
    fun `stored values round-trip`() {
        JobStatus.entries.forEach { status ->
            assertEquals(status, JobStatus.fromStored(status.storedValue))
        }
    }
}
