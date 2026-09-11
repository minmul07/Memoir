package minmul.memoir.core.storage

import androidx.sqlite.SQLiteException
import app.cash.turbine.test
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
import java.util.concurrent.ConcurrentLinkedQueue

class MemoirDatabaseTest {
    @Test
    fun `queue observation emits saved images and removes deleted jobs`() = runDatabaseTest { db ->
        db.analysisJobDao().observeQueue().test {
            assertTrue(awaitItem().isEmpty())
            db.contentWriteDao().insertItemsAndJobs(
                listOf(ItemJobWrite(itemEntity("image"), jobEntity("job", "image"))),
            )
            assertEquals(
                listOf(QueueEntry("job", "image", "items/image/original", JobStatus.Queued)),
                awaitItem(),
            )
            db.analysisJobDao().deleteById("job")
            assertTrue(awaitItem().isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `queue includes only active jobs in queue order`() = runDatabaseTest { db ->
        JobStatus.entries.forEachIndexed { index, status ->
            val id = status.storedValue
            db.itemDao().insert(itemEntity(id))
            db.analysisJobDao().insert(
                jobEntity(id, id, queueOrder = 10 - index).copy(status = status),
            )
        }
        db.analysisJobDao().observeQueue().test {
            assertEquals(listOf("running", "queued"), awaitItem().map { it.jobId })
            cancelAndIgnoreRemainingEvents()
        }
    }

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
        db.analysisJobDao()
            .insert(jobEntity(id = "job-existing", itemId = "existing", queueOrder = 4))

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

    @Test
    fun `failed job retries behind fresh work and second failure is terminal`() =
        runDatabaseTest { db ->
            db.contentWriteDao().insertItemsAndJobs(
                listOf(
                    ItemJobWrite(itemEntity("a"), jobEntity("a", "a")),
                    ItemJobWrite(itemEntity("b"), jobEntity("b", "b")),
                )
            )
            val dao = db.analysisWorkDao()
            assertEquals("a", dao.claimNext(2)?.jobId)
            dao.fail("a", "fake", 3)
            assertEquals("b", dao.claimNext(4)?.jobId)
            dao.complete("b", "한국어", "{}", 5)
            assertEquals("a", dao.claimNext(6)?.jobId)
            dao.fail("a", "fake", 7)
            assertNull(dao.claimNext(8))
            assertEquals(JobStatus.Failed, dao.job("a")?.status)
            assertEquals(1, dao.job("a")?.attemptCount)
            assertNull(db.analysisResultDao().getByItemId("a"))
            assertEquals("한국어", db.analysisResultDao().getByItemId("b")?.ocrText)
        }

    @Test
    fun `completion cannot resurrect cancelled or deleted jobs`() = runDatabaseTest { db ->
        for (id in listOf("cancelled", "deleted", "queue")) {
            db.contentWriteDao()
                .insertItemsAndJobs(listOf(ItemJobWrite(itemEntity(id), jobEntity(id, id))))
            db.analysisWorkDao().claimNext(2)
            when (id) {
                "cancelled" -> db.analysisWorkDao().cancel(id, 3)
                "deleted" -> db.itemDao().deleteById(id)
                else -> db.analysisWorkDao().deleteQueue()
            }
            db.analysisWorkDao().complete(id, "text", "{}", 4)
            assertNull(db.analysisResultDao().getByItemId(id))
        }
        assertEquals(JobStatus.Cancelled, db.analysisWorkDao().job("cancelled")?.status)
        assertNull(db.itemDao().getById("deleted"))
        assertNotNull(db.itemDao().getById("queue"))
    }

    @Test
    fun `successful completion exposes detail and history together`() = runDatabaseTest { db ->
        db.contentWriteDao()
            .insertItemsAndJobs(listOf(ItemJobWrite(itemEntity("a"), jobEntity("a", "a"))))
        val dao = db.analysisWorkDao()
        dao.claimNext(2)
        dao.complete("a", "한국어", "{\"fake\":true}", 3)
        dao.observeDetail("a").test {
            val detail = awaitItem()
            assertEquals(JobStatus.Succeeded, detail?.status)
            assertEquals("한국어", detail?.ocrText)
            assertEquals("{\"fake\":true}", detail?.payloadJson)
            assertEquals(1L, detail?.createdAt)
            cancelAndIgnoreRemainingEvents()
        }
        dao.observeHistory().test {
            assertEquals(listOf("a"), awaitItem().map { it.itemId })
            cancelAndIgnoreRemainingEvents()
        }
        db.itemDao().deleteById("a")
        assertNull(dao.job("a"))
        assertNull(db.analysisResultDao().getByItemId("a"))
    }

    @Test
    fun `recovery fails interrupted jobs without retrying them`() = runDatabaseTest { db ->
        db.contentWriteDao().insertItemsAndJobs(
            listOf(
                ItemJobWrite(itemEntity("a"), jobEntity("a", "a")),
                ItemJobWrite(itemEntity("b"), jobEntity("b", "b")),
            )
        )
        val dao = db.analysisWorkDao()
        dao.claimNext(2)
        dao.recoverInterrupted(3)
        assertEquals(JobStatus.Failed, dao.job("a")?.status)
        assertEquals("interrupted", dao.job("a")?.errorMessage)
        assertEquals("b", dao.claimNext(4)?.jobId)
    }

    @Test
    fun `concurrent claims select different jobs`() = runDatabaseTest { db ->
        db.contentWriteDao().insertItemsAndJobs(
            listOf(
                ItemJobWrite(itemEntity("a"), jobEntity("a", "a")),
                ItemJobWrite(itemEntity("b"), jobEntity("b", "b")),
            )
        )
        val selected = ConcurrentLinkedQueue<String>()
        coroutineScope {
            repeat(4) {
                launch(Dispatchers.Default) {
                    db.analysisWorkDao().claimNext(2)?.let { selected.add(it.jobId) }
                }
            }
        }
        assertEquals(setOf("a", "b"), selected.toSet())
        assertEquals(2, selected.size)
    }

    @Test
    fun `hasQueuedWork is true only for queued jobs and does not claim`() = runDatabaseTest { db ->
        val dao = db.analysisWorkDao()
        assertEquals(false, dao.hasQueuedWork())
        db.contentWriteDao()
            .insertItemsAndJobs(listOf(ItemJobWrite(itemEntity("a"), jobEntity("a", "a"))))
        assertEquals(true, dao.hasQueuedWork())
        assertEquals(JobStatus.Queued, dao.job("a")?.status)
        dao.claimNext(2)
        assertEquals(false, dao.hasQueuedWork())
        assertEquals(JobStatus.Running, dao.job("a")?.status)
    }

    @Test
    fun `failActiveQueue marks queued and running jobs failed without retry`() =
        runDatabaseTest { db ->
            db.contentWriteDao().insertItemsAndJobs(
                listOf(
                    ItemJobWrite(itemEntity("a"), jobEntity("a", "a")),
                    ItemJobWrite(itemEntity("b"), jobEntity("b", "b")),
                )
            )
            val dao = db.analysisWorkDao()
            dao.claimNext(2)
            dao.failActiveQueue("model_load_failed", 3)
            assertEquals(JobStatus.Failed, dao.job("a")?.status)
            assertEquals(JobStatus.Failed, dao.job("b")?.status)
            assertEquals("model_load_failed", dao.job("a")?.errorMessage)
            assertEquals("model_load_failed", dao.job("b")?.errorMessage)
            assertEquals(0, dao.job("a")?.attemptCount)
            assertEquals(0, dao.job("b")?.attemptCount)
            assertNull(dao.claimNext(4))
        }

    @Test
    fun `item list includes created time and analysis payload`() = runDatabaseTest { db ->
        db.contentWriteDao()
            .insertItemsAndJobs(listOf(ItemJobWrite(itemEntity("a"), jobEntity("a", "a"))))
        val dao = db.analysisWorkDao()
        dao.claimNext(2)
        dao.complete("a", "한국어", """{"title":"T","detailed_summary":"D"}""", 3)
        dao.observeItems().test {
            val item = awaitItem().single()
            assertEquals("a", item.itemId)
            assertEquals(1L, item.createdAt)
            assertEquals("""{"title":"T","detailed_summary":"D"}""", item.payloadJson)
            assertEquals("한국어", item.ocrText)
            cancelAndIgnoreRemainingEvents()
        }
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
