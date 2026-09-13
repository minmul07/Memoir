package minmul.memoir.feature.queue

import minmul.memoir.core.model.JobStatus
import minmul.memoir.core.model.QueueItem
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class WorkQueueStartEnabledTest {
    @Test
    fun `start is enabled only while the queue is waiting`() {
        val queued = QueueItem("1", "1", "preview", JobStatus.Queued)
        val running = QueueItem("2", "2", "preview", JobStatus.Running)

        assertTrue(workQueueStartEnabled(listOf(queued), isLoading = false, failed = false))
        assertFalse(workQueueStartEnabled(emptyList(), isLoading = false, failed = false))
        assertFalse(workQueueStartEnabled(listOf(running), isLoading = false, failed = false))
        assertFalse(
            workQueueStartEnabled(
                listOf(queued, running),
                isLoading = false,
                failed = false
            )
        )
        assertFalse(workQueueStartEnabled(listOf(queued), isLoading = true, failed = false))
        assertFalse(workQueueStartEnabled(listOf(queued), isLoading = false, failed = true))
    }
}
