package minmul.memoir.background.analysis

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import minmul.memoir.core.ai.OcrEngine
import minmul.memoir.core.model.GemmaModel
import minmul.memoir.core.model.JobStatus
import minmul.memoir.core.model.LlmRuntimeStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalCoroutinesApi::class)
class AnalysisRunnerTest {
    @Test
    fun `first failure retries after remaining items and stops after second failure`() = runTest {
        val repository = FakeAnalysisRepository(listOf("a", "b", "c"))
        val visits = mutableListOf<String>()
        val ocr = object : OcrEngine {
            override suspend fun recognize(imagePath: String): String {
                visits += imagePath
                delay(10.milliseconds)
                return "한국어"
            }
        }
        val llm = FakeLlmEngine { path, text ->
            assertEquals("한국어", text)
            if (File(path).name == "a") error("fake")
            VALID_PAYLOAD
        }
        analysisRunner(repository, ocr, llm).drain()

        assertEquals(listOf("a", "b", "c", "a"), visits)
        assertEquals(listOf("b", "c"), repository.completed)
        assertEquals(JobStatus.Failed, repository.jobs.value.first().status)
        assertEquals(1, repository.jobs.value.first().attemptCount)
        assertEquals(1, llm.closeCalls)
        assertEquals(LlmRuntimeStatus.Idle, llm.status.value)
    }

    @Test
    fun `successful retry completes the same job once`() = runTest {
        val repository = FakeAnalysisRepository(listOf("a"))
        var attempts = 0
        val llm = FakeLlmEngine { _, _ ->
            if (attempts++ == 0) error("fake")
            VALID_PAYLOAD
        }
        analysisRunner(repository, successfulOcr(), llm).drain()
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
        val llm = FakeLlmEngine()
        val task = launch { analysisRunner(repository, ocr, llm).drain() }
        entered.await()
        repository.cancel("a")
        task.join()
        assertEquals(listOf("b"), llm.summarizeCalls.map { File(it.first).name })
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
        val llm = FakeLlmEngine { _, _ -> fail("must not infer") }
        val task = launch { analysisRunner(repository, ocr, llm).drain() }
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
                delay(10.milliseconds)
                active--
                return "text"
            }
        }
        val llm = FakeLlmEngine()
        val drain = analysisRunner(repository, ocr, llm)
        coroutineScope { repeat(3) { launch { drain.drain() } } }
        assertEquals(1, maxActive)
        assertEquals(listOf("a", "b"), repository.completed)
        assertEquals(1, llm.loadCalls)
    }

    @Test
    fun `interrupted work becomes failed and queued work continues`() = runTest {
        val repository = FakeAnalysisRepository(listOf("a", "b"))
        repository.claimNext()
        analysisRunner(repository, successfulOcr(), FakeLlmEngine()).drain()
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
        val llm = FakeLlmEngine { _, _ -> fail("must not infer") }
        analysisRunner(repository, ocr, llm).drain()
        assertEquals(2, calls)
        assertEquals(JobStatus.Failed, repository.jobs.value.single().status)
        assertEquals(1, llm.loadCalls)
        assertEquals(1, llm.closeCalls)
    }

    @Test
    fun `empty queue does not load the model`() = runTest {
        val repository = FakeAnalysisRepository(emptyList())
        val llm = FakeLlmEngine()
        AnalysisRunner(
            repository,
            repository,
            successfulOcr(),
            llm,
            { fail("must not locate") },
            ::imageFile,
        ).drain()
        assertEquals(0, llm.loadCalls)
        assertEquals(0, llm.closeCalls)
        assertEquals(LlmRuntimeStatus.Idle, llm.status.value)
    }

    @Test
    fun `missing model leaves jobs queued without claiming`() = runTest {
        val repository = FakeAnalysisRepository(listOf("a", "b"))
        val llm = FakeLlmEngine()
        var ocrCalls = 0
        val ocr = object : OcrEngine {
            override suspend fun recognize(imagePath: String): String {
                ocrCalls++
                return "text"
            }
        }
        AnalysisRunner(
            repository,
            repository,
            ocr,
            llm,
            { null },
            ::imageFile,
        ).drain()
        assertEquals(0, llm.loadCalls)
        assertEquals(0, ocrCalls)
        assertEquals(LlmRuntimeStatus.Missing, llm.status.value)
        assertEquals(
            listOf(JobStatus.Queued, JobStatus.Queued),
            repository.jobs.value.map { it.status })
        assertTrue(repository.completed.isEmpty())
    }

    @Test
    fun `load failure marks the whole queue terminal failed without OCR`() = runTest {
        val repository = FakeAnalysisRepository(listOf("a", "b"))
        val llm = FakeLlmEngine(loadError = IllegalStateException("gpu"))
        var ocrCalls = 0
        val ocr = object : OcrEngine {
            override suspend fun recognize(imagePath: String): String {
                ocrCalls++
                return "text"
            }
        }
        analysisRunner(repository, ocr, llm).drain()
        assertEquals(1, llm.loadCalls)
        assertEquals(0, ocrCalls)
        assertEquals(0, llm.closeCalls)
        assertEquals(
            LlmRuntimeStatus.Failed(GemmaModel.E2B, "IllegalStateException"),
            llm.status.value
        )
        assertEquals(
            listOf(JobStatus.Failed, JobStatus.Failed),
            repository.jobs.value.map { it.status })
        assertEquals(
            listOf("model_load_failed", "model_load_failed"),
            repository.jobs.value.map { it.errorMessage })
        assertEquals(listOf(0, 0), repository.jobs.value.map { it.attemptCount })
        assertTrue(repository.completed.isEmpty())
    }

    @Test
    fun `successful item passes absolute image path and OCR text to summarize`() = runTest {
        val repository = FakeAnalysisRepository(listOf("a"))
        val llm = FakeLlmEngine()
        analysisRunner(repository, successfulOcr(), llm).drain()
        assertEquals(listOf(imageFile("a").absolutePath to "text"), llm.summarizeCalls)
        assertEquals(listOf("a"), repository.completed)
        assertEquals(listOf("""{"title":"T","detailed_summary":"D"}"""), repository.payloads)
        assertEquals(1, llm.closeCalls)
        assertEquals(LlmRuntimeStatus.Idle, llm.status.value)
    }

    @Test
    fun `invalid structured output fails the job without completing`() = runTest {
        val repository = FakeAnalysisRepository(listOf("a"))
        val raw = """{"summary":"legacy"}"""
        val llm = FakeLlmEngine { _, _ -> raw }
        val logs = mutableListOf<String>()
        analysisRunner(repository, successfulOcr(), llm, logs).drain()
        assertTrue(repository.completed.isEmpty())
        assertTrue(repository.payloads.isEmpty())
        assertEquals(JobStatus.Failed, repository.jobs.value.single().status)
        assertEquals(1, repository.jobs.value.single().attemptCount)
        assertTrue(
            logs.any {
                it.contains("stage=infer parse_failed") &&
                        it.contains("payloadChars=${raw.length}") &&
                        it.contains("reason=missing_title")
            },
        )
    }


    @Test
    fun `cancel during inference finishes cleanup before continuing queue`() = runTest {
        val repository = FakeAnalysisRepository(listOf("a", "b"))
        val entered = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val llm = FakeLlmEngine { path, _ ->
            if (File(path).name == "a") {
                entered.complete(Unit)
                try {
                    awaitCancellation()
                } finally {
                    withContext(NonCancellable) { release.await() }
                }
            }
            VALID_PAYLOAD
        }
        val task = launch { analysisRunner(repository, successfulOcr(), llm).drain() }
        entered.await()
        repository.cancel("a")
        runCurrent()
        assertEquals(listOf("a"), llm.summarizeCalls.map { File(it.first).name })
        assertTrue(repository.completed.isEmpty())

        release.complete(Unit)
        task.join()
        assertEquals(listOf("b"), repository.completed)
        assertEquals(JobStatus.Cancelled, repository.jobs.value.first().status)
        assertEquals(1, llm.closeCalls)
    }

    private fun successfulOcr() = object : OcrEngine {
        override suspend fun recognize(imagePath: String) = "text"
    }

    private fun analysisRunner(
        repository: FakeAnalysisRepository,
        ocr: OcrEngine,
        llm: FakeLlmEngine,
        logs: MutableList<String> = mutableListOf(),
        locate: ReadyGemmaModelLocator = ReadyGemmaModelLocator {
            LocatedGemmaModel(GemmaModel.E2B, File("gemma-4-E2B-it.litertlm"))
        },
    ) = AnalysisRunner(repository, repository, ocr, llm, locate, ::imageFile, logs::add)

    private fun imageFile(relative: String) = File("/memoir-files", relative)
}
