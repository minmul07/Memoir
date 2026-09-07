package minmul.memoir.background.analysis

import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import minmul.memoir.core.ai.OcrEngine
import minmul.memoir.core.model.JobStatus
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AnalysisRunnerTest {
    @Test
    fun `first failure retries after remaining items and stops after second failure`() = runTest {
        val repository = FakeAnalysisRepository(listOf("a", "b", "c"))
        val visits = mutableListOf<String>()
        val ocr = object : OcrEngine {
            override suspend fun recognize(imagePath: String): String {
                visits += imagePath
                delay(10)
                return "한국어"
            }
        }
        val runner = AnalysisRunner(repository, repository, ocr, FakeAnalysis { item, text ->
            assertEquals("한국어", text)
            if (item.itemId == "a") error("fake")
            "{}"
        })

        runner.drain()

        assertEquals(listOf("a", "b", "c", "a"), visits)
        assertEquals(listOf("b", "c"), repository.completed)
        assertEquals(JobStatus.Failed, repository.jobs.value.first().status)
        assertEquals(1, repository.jobs.value.first().attemptCount)
    }

    @Test
    fun `successful retry completes the same job once`() = runTest {
        val repository = FakeAnalysisRepository(listOf("a"))
        val runner = AnalysisRunner(repository, repository, successfulOcr(), FakeAnalysis { item, _ ->
            if (item.attemptCount == 0) error("fake")
            "{}"
        })
        runner.drain()
        assertEquals(listOf("a"), repository.completed)
        assertEquals(1, repository.jobs.value.single().attemptCount)
        assertEquals(JobStatus.Succeeded, repository.jobs.value.single().status)
    }

    @Test
    fun `cancel during OCR skips inference and continues queue`() = runTest {
        val repository = FakeAnalysisRepository(listOf("a", "b"))
        val entered = CompletableDeferred<Unit>()
        val ocr = object : OcrEngine {
            override suspend fun recognize(imagePath: String): String {
                if (imagePath == "a") {
                    entered.complete(Unit)
                    awaitCancellation()
                }
                return "text"
            }
        }
        val inferred = mutableListOf<String>()
        val runner = AnalysisRunner(repository, repository, ocr, FakeAnalysis { item, _ ->
            inferred += item.itemId
            "{}"
        })
        val task = launch { runner.drain() }
        entered.await()
        repository.cancel("a")
        task.join()
        assertEquals(listOf("b"), inferred)
        assertEquals(JobStatus.Cancelled, repository.jobs.value.first().status)
    }

    @Test
    fun `deleting all items during OCR does not save late results`() = runTest {
        val repository = FakeAnalysisRepository(listOf("a", "b"))
        val entered = CompletableDeferred<Unit>()
        val released = CompletableDeferred<Unit>()
        val ocr = object : OcrEngine {
            override suspend fun recognize(imagePath: String): String {
                entered.complete(Unit)
                withContext(NonCancellable) { released.await() }
                currentCoroutineContext().ensureActive()
                return "text"
            }
        }
        val runner = AnalysisRunner(repository, repository, ocr, FakeAnalysis { _, _ -> fail("must not infer") })
        val task = launch { runner.drain() }
        entered.await()
        repository.deleteAllItems()
        runCurrent()
        released.complete(Unit)
        task.join()
        assertTrue(repository.completed.isEmpty())
        assertTrue(repository.jobs.value.isEmpty())
    }

    @Test
    fun `multiple drain requests never overlap OCR`() = runTest {
        val repository = FakeAnalysisRepository(listOf("a", "b"))
        var active = 0
        var maxActive = 0
        val ocr = object : OcrEngine {
            override suspend fun recognize(imagePath: String): String {
                active++
                maxActive = maxOf(maxActive, active)
                delay(10)
                active--
                return "text"
            }
        }
        val runner = AnalysisRunner(repository, repository, ocr, FakeAnalysis { _, _ -> "{}" })
        coroutineScope { repeat(3) { launch { runner.drain() } } }
        assertEquals(1, maxActive)
        assertEquals(listOf("a", "b"), repository.completed)
    }

    @Test
    fun `interrupted work becomes failed and queued work continues`() = runTest {
        val repository = FakeAnalysisRepository(listOf("a", "b"))
        repository.claimNext()
        AnalysisRunner(repository, repository, successfulOcr(), FakeAnalysis { _, _ -> "{}" }).drain()
        assertEquals(JobStatus.Failed, repository.jobs.value.first().status)
        assertEquals(listOf("b"), repository.completed)
    }

    @Test
    fun `OCR failure is retried once without inference`() = runTest {
        val repository = FakeAnalysisRepository(listOf("a"))
        var calls = 0
        val ocr = object : OcrEngine {
            override suspend fun recognize(imagePath: String): String {
                calls++
                error("ocr unavailable")
            }
        }
        AnalysisRunner(repository, repository, ocr, FakeAnalysis { _, _ -> fail("must not infer") }).drain()
        assertEquals(2, calls)
        assertEquals(JobStatus.Failed, repository.jobs.value.single().status)
    }

    private fun successfulOcr() = object : OcrEngine {
        override suspend fun recognize(imagePath: String) = "text"
    }
}
