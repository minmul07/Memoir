package minmul.memoir.core.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ItemSourceTest {
    @Test
    fun `stored values round-trip`() {
        ItemSource.entries.forEach { source ->
            assertEquals(source, ItemSource.fromStored(source.storedValue))
        }
    }
}
