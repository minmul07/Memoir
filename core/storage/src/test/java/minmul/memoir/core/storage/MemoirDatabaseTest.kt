package minmul.memoir.core.storage

import androidx.sqlite.SQLiteException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import minmul.memoir.core.model.ItemSource
import minmul.memoir.core.model.JobStage
import minmul.memoir.core.model.JobStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MemoirDatabaseTest {
    @Test
    fun `second job with the same item id fails`() = runDatabaseTest { db ->
        db.itemDao().insert(itemEntity("item-1"))
        db.analysisJobDao().insert(jobEntity(id = "job-1", itemId = "item-1"))

        val error = runCatching {
            db.analysisJobDao().insert(jobEntity(id = "job-2", itemId = "item-1"))
        }.exceptionOrNull()

        assertTrue(error is SQLiteException)
    }

    @Test
    fun `deleting an item cascades job and analysis`() = runDatabaseTest { db ->
        db.itemDao().insert(itemEntity("item-1"))
        db.analysisJobDao().insert(jobEntity(id = "job-1", itemId = "item-1"))
        db.analysisResultDao().insert(analysisEntity(itemId = "item-1", jobId = "job-1"))

        db.itemDao().deleteById("item-1")

        assertNull(db.itemDao().getById("item-1"))
        assertNull(db.analysisJobDao().getByItemId("item-1"))
        assertNull(db.analysisResultDao().getByItemId("item-1"))
    }

    @Test
    fun `deleting a job keeps item and analysis`() = runDatabaseTest { db ->
        db.itemDao().insert(itemEntity("item-1"))
        db.analysisJobDao().insert(jobEntity(id = "job-1", itemId = "item-1"))
        db.analysisResultDao().insert(analysisEntity(itemId = "item-1", jobId = "job-1"))

        db.analysisJobDao().deleteById("job-1")

        assertNotNull(db.itemDao().getById("item-1"))
        assertNull(db.analysisJobDao().getById("job-1"))
        assertEquals("job-1", db.analysisResultDao().getByItemId("item-1")?.jobId)
    }

    @Test
    fun `max queue order is null when empty and increases with inserts`() = runDatabaseTest { db ->
        assertNull(db.analysisJobDao().maxQueueOrder())

        db.itemDao().insert(itemEntity("item-1"))
        db.analysisJobDao().insert(jobEntity(id = "job-1", itemId = "item-1", queueOrder = 3))
        db.itemDao().insert(itemEntity("item-2"))
        db.analysisJobDao().insert(jobEntity(id = "job-2", itemId = "item-2", queueOrder = 8))

        assertEquals(8, db.analysisJobDao().maxQueueOrder())
    }

    @Test
    fun `item and job insert assigns queue orders after current max`() = runDatabaseTest { db ->
        db.itemDao().insert(itemEntity("existing"))
        db.analysisJobDao().insert(jobEntity(id = "job-existing", itemId = "existing", queueOrder = 4))

        db.contentWriteDao().insertItemsAndJobs(
            listOf(
                ItemJobWrite(
                    item = itemEntity("item-1"),
                    job = jobEntity(id = "job-1", itemId = "item-1", queueOrder = 99),
                ),
                ItemJobWrite(
                    item = itemEntity("item-2"),
                    job = jobEntity(id = "job-2", itemId = "item-2", queueOrder = 99),
                ),
            ),
        )

        assertEquals(5, db.analysisJobDao().getById("job-1")?.queueOrder)
        assertEquals(6, db.analysisJobDao().getById("job-2")?.queueOrder)
    }

    @Test
    fun `concurrent item and job inserts get distinct sequential queue orders`() = runTest {
        val db = MemoirDatabase.createInMemory(Dispatchers.IO)
        try {
            val count = 20
            val start = CompletableDeferred<Unit>()
            coroutineScope {
                repeat(count) { index ->
                    launch(Dispatchers.IO) {
                        start.await()
                        db.contentWriteDao().insertItemsAndJobs(
                            listOf(
                                ItemJobWrite(
                                    item = itemEntity("item-$index"),
                                    job = jobEntity(id = "job-$index", itemId = "item-$index"),
                                ),
                            ),
                        )
                    }
                }
                start.complete(Unit)
            }

            val orders = (0 until count).map { index ->
                requireNotNull(db.analysisJobDao().getById("job-$index")).queueOrder
            }
            assertEquals(count, orders.toSet().size)
            assertEquals((0 until count).toList(), orders.sorted())
        } finally {
            db.close()
        }
    }

    @Test
    fun `item and job batch insert rolls back together`() = runDatabaseTest { db ->
        val error = runCatching {
            db.contentWriteDao().insertItemsAndJobs(
                listOf(
                    ItemJobWrite(
                        item = itemEntity("item-1"),
                        job = jobEntity(id = "job-1", itemId = "item-1"),
                    ),
                    ItemJobWrite(
                        item = itemEntity("item-2"),
                        job = jobEntity(id = "job-2", itemId = "item-1"),
                    ),
                ),
            )
        }.exceptionOrNull()

        assertTrue(error is SQLiteException)
        assertNull(db.itemDao().getById("item-1"))
        assertNull(db.itemDao().getById("item-2"))
        assertNull(db.analysisJobDao().getById("job-1"))
        assertNull(db.analysisJobDao().getById("job-2"))
    }

    private fun runDatabaseTest(testBody: suspend TestScope.(MemoirDatabase) -> Unit) = runTest {
        val db = MemoirDatabase.createInMemory(StandardTestDispatcher(testScheduler))
        try {
            testBody(db)
        } finally {
            db.close()
        }
    }

    private fun itemEntity(id: String) = ItemEntity(
        id = id,
        createdAt = 1L,
        source = ItemSource.Share,
        filePath = "items/$id/original",
        mimeType = "image/jpeg",
    )

    private fun jobEntity(id: String, itemId: String, queueOrder: Int = 0) = AnalysisJobEntity(
        id = id,
        itemId = itemId,
        status = JobStatus.Queued,
        stage = JobStage.Waiting,
        queueOrder = queueOrder,
        attemptCount = 0,
        errorMessage = null,
        createdAt = 1L,
        startedAt = null,
        finishedAt = null,
    )

    private fun analysisEntity(itemId: String, jobId: String) = AnalysisResultEntity(
        itemId = itemId,
        jobId = jobId,
        startedAt = 1L,
        completedAt = 2L,
        ocrText = null,
        payloadJson = "{}",
    )
}
