package minmul.memoir.core.ai

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import minmul.memoir.core.model.OcrModel
import minmul.memoir.core.model.OcrModelState
import minmul.memoir.core.model.OcrModelStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultOcrModelManagerTest {
    @Test
    fun `refresh reports availability and failures independently for each model`() = runTest {
        val backend = FakeOcrModelBackend().apply {
            available += OcrModel.Korean
            checkFailures += OcrModel.Chinese
        }
        val manager = DefaultOcrModelManager(backend, backgroundScope)

        manager.refresh()

        assertEquals(5, manager.models.value.size)
        assertEquals(OcrModelStatus.Ready, manager.state(OcrModel.Korean).status)
        assertEquals(OcrModelStatus.Failed, manager.state(OcrModel.Chinese).status)
        assertEquals(OcrModelStatus.Missing, manager.state(OcrModel.Japanese).status)
        assertTrue(backend.downloads.isEmpty())
    }

    @Test
    fun `individual download publishes progress without refresh overwriting it`() = runTest {
        val gate = CompletableDeferred<Unit>()
        val backend = FakeOcrModelBackend().apply { gates[OcrModel.Japanese] = gate }
        val manager = DefaultOcrModelManager(backend, backgroundScope)
        val download = async { manager.install(OcrModel.Japanese) }
        runCurrent()

        val progress = OcrModelState(OcrModel.Japanese, OcrModelStatus.Downloading, 42, 100)
        backend.progress.getValue(OcrModel.Japanese)(progress)
        manager.refresh()

        assertEquals(progress, manager.state(OcrModel.Japanese))
        assertEquals(0.42f, manager.state(OcrModel.Japanese).progress)
        assertEquals(OcrModelStatus.Missing, manager.state(OcrModel.Korean).status)

        gate.complete(Unit)
        download.await()
        assertEquals(OcrModelStatus.Ready, manager.state(OcrModel.Japanese).status)
        assertEquals(listOf(OcrModel.Japanese), backend.downloads)
    }

    @Test
    fun `simultaneous callers share one download and cancellation only removes a waiter`() =
        runTest {
            val gate = CompletableDeferred<Unit>()
            val backend = FakeOcrModelBackend().apply { gates[OcrModel.Korean] = gate }
            val manager = DefaultOcrModelManager(backend, backgroundScope)
            val settings = async { manager.install(OcrModel.Korean) }
            val analysis = async { manager.install(OcrModel.Korean) }
            runCurrent()
            settings.cancelAndJoin()

            assertEquals(listOf(OcrModel.Korean), backend.downloads)
            assertTrue(manager.state(OcrModel.Korean).isDownloading)
            assertFalse(analysis.isCompleted)

            gate.complete(Unit)
            analysis.await()
            assertEquals(OcrModelStatus.Ready, manager.state(OcrModel.Korean).status)
        }

    @Test
    fun `download survives when its only screen waiter is cancelled`() = runTest {
        val gate = CompletableDeferred<Unit>()
        val backend = FakeOcrModelBackend().apply { gates[OcrModel.Latin] = gate }
        val manager = DefaultOcrModelManager(backend, backgroundScope)
        val screen = async { manager.install(OcrModel.Latin) }
        runCurrent()
        screen.cancelAndJoin()

        gate.complete(Unit)
        runCurrent()

        assertEquals(OcrModelStatus.Ready, manager.state(OcrModel.Latin).status)
    }

    @Test
    fun `different models download independently and a failure can be retried`() = runTest {
        val gate = CompletableDeferred<Unit>()
        val backend = FakeOcrModelBackend().apply {
            gates[OcrModel.Korean] = gate
            installFailures += OcrModel.Japanese
        }
        val manager = DefaultOcrModelManager(backend, backgroundScope)
        val korean = async { manager.install(OcrModel.Korean) }
        val japanese = runCatching { manager.install(OcrModel.Japanese) }

        assertTrue(japanese.isFailure)
        assertEquals(OcrModelStatus.Failed, manager.state(OcrModel.Japanese).status)
        assertTrue(manager.state(OcrModel.Korean).isDownloading)
        assertFalse(korean.isCompleted)

        backend.installFailures.clear()
        manager.install(OcrModel.Japanese)
        assertEquals(OcrModelStatus.Ready, manager.state(OcrModel.Japanese).status)
        gate.complete(Unit)
        korean.await()
        assertEquals(2, backend.downloads.count { it == OcrModel.Japanese })
    }

    @Test
    fun `already installed model does not start another download`() = runTest {
        val backend = FakeOcrModelBackend().apply { available += OcrModel.Chinese }
        val manager = DefaultOcrModelManager(backend, backgroundScope)

        manager.install(OcrModel.Chinese)

        assertTrue(backend.downloads.isEmpty())
        assertEquals(OcrModelStatus.Ready, manager.state(OcrModel.Chinese).status)
    }

    @Test
    fun `installation is not ready until availability is verified`() = runTest {
        val backend = object : OcrModelBackend {
            override suspend fun isAvailable(model: OcrModel) = false
            override suspend fun install(model: OcrModel, onProgress: (OcrModelState) -> Unit) =
                Unit
        }
        val manager = DefaultOcrModelManager(backend, backgroundScope)

        val result = runCatching { manager.install(OcrModel.Latin) }

        assertTrue(result.isFailure)
        assertEquals(OcrModelStatus.Failed, manager.state(OcrModel.Latin).status)
    }

    @Test
    fun `progress stays indeterminate for unknown size and is bounded when size is known`() {
        assertNull(OcrModelState(OcrModel.Latin, totalBytes = null).progress)
        assertNull(OcrModelState(OcrModel.Latin, totalBytes = 0).progress)
        assertEquals(
            1f,
            OcrModelState(OcrModel.Latin, downloadedBytes = 200, totalBytes = 100).progress
        )
        assertEquals(
            0f,
            OcrModelState(OcrModel.Latin, downloadedBytes = -10, totalBytes = 100).progress
        )
    }

    private fun OcrModelManager.state(model: OcrModel) = models.value.first { it.model == model }
}
