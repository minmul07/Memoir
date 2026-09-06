package minmul.memoir.core.storage

import androidx.sqlite.SQLiteException
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

    private fun jobEntity(id: String, itemId: String) = AnalysisJobEntity(
        id = id,
        itemId = itemId,
        status = JobStatus.Queued,
        stage = JobStage.Waiting,
        queueOrder = 0,
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
