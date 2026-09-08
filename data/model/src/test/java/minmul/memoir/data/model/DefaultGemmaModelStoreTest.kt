package minmul.memoir.data.model

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import minmul.memoir.core.model.GemmaModel
import minmul.memoir.core.model.GemmaModelStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultGemmaModelStoreTest {
    @TempDir
    lateinit var directory: File

    @Test
    fun `refresh reports availability independently and does not start downloads`() = runTest {
        val files = files()
        files.destination(GemmaModel.E4B).writeBytes(ByteArray(32))
        val store = store(FakeGemmaDownloadEngine(), files)

        store.refresh()

        assertEquals(GemmaModelStatus.Ready, store.state(GemmaModel.E4B).status)
        assertEquals(GemmaModelStatus.Missing, store.state(GemmaModel.E2B).status)
    }

    @Test
    fun `individual download publishes progress without refresh overwriting it`() = runTest {
        val engine = FakeGemmaDownloadEngine()
        val store = store(engine)
        val download = async { store.install(GemmaModel.E2B) }
        runCurrent()
        val id = engine.enqueuedId(GemmaModel.E2B)
        engine.set(id, GemmaDownloadStatus.Running, 42, 100)
        runCurrent()
        store.refresh()

        assertEquals(GemmaModelStatus.Downloading, store.state(GemmaModel.E2B).status)
        assertEquals(0.42f, store.state(GemmaModel.E2B).progress)
        assertEquals(GemmaModelStatus.Missing, store.state(GemmaModel.E4B).status)

        engine.succeed(id)
        download.await()
        assertEquals(GemmaModelStatus.Ready, store.state(GemmaModel.E2B).status)
        assertEquals(listOf(GemmaModel.E2B), engine.enqueued)
    }

    @Test
    fun `simultaneous callers share one download`() = runTest {
        val engine = FakeGemmaDownloadEngine()
        val store = store(engine)
        val first = async { store.install(GemmaModel.E4B) }
        val second = async { store.install(GemmaModel.E4B) }
        runCurrent()

        assertEquals(listOf(GemmaModel.E4B), engine.enqueued)
        engine.succeed(engine.enqueuedId(GemmaModel.E4B))
        first.await()
        second.await()
        assertEquals(GemmaModelStatus.Ready, store.state(GemmaModel.E4B).status)
    }

    @Test
    fun `different models download independently and a failure can be retried`() = runTest {
        val engine = FakeGemmaDownloadEngine()
        val store = store(engine)
        val e4b = async { store.install(GemmaModel.E4B) }
        val e2b = async { runCatching { store.install(GemmaModel.E2B) } }
        runCurrent()
        engine.set(engine.enqueuedId(GemmaModel.E2B), GemmaDownloadStatus.Failed)
        advanceUntilIdle()

        assertTrue(e2b.await().isFailure)
        assertEquals(GemmaModelStatus.Failed, store.state(GemmaModel.E2B).status)
        assertTrue(store.state(GemmaModel.E4B).isDownloading)

        val retry = async { store.install(GemmaModel.E2B) }
        runCurrent()
        engine.succeed(engine.lastId(GemmaModel.E2B))
        retry.await()
        engine.succeed(engine.lastId(GemmaModel.E4B))
        e4b.await()

        assertEquals(GemmaModelStatus.Ready, store.state(GemmaModel.E2B).status)
        assertEquals(GemmaModelStatus.Ready, store.state(GemmaModel.E4B).status)
        assertEquals(2, engine.enqueued.count { it == GemmaModel.E2B })
    }

    @Test
    fun `already installed model does not start another download`() = runTest {
        val files = files()
        files.destination(GemmaModel.E2B).writeBytes(ByteArray(32))
        val engine = FakeGemmaDownloadEngine()
        val store = store(engine, files)

        store.install(GemmaModel.E2B)

        assertTrue(engine.enqueued.isEmpty())
        assertEquals(GemmaModelStatus.Ready, store.state(GemmaModel.E2B).status)
    }

    @Test
    fun `successful download is not ready until the file meets the size floor`() = runTest {
        val engine = FakeGemmaDownloadEngine()
        val store = store(engine)
        val download = async { runCatching { store.install(GemmaModel.E2B) } }
        runCurrent()
        engine.succeed(engine.enqueuedId(GemmaModel.E2B), bytes = 1)
        advanceUntilIdle()

        assertTrue(download.await().isFailure)
        assertEquals(GemmaModelStatus.Failed, store.state(GemmaModel.E2B).status)
    }

    @Test
    fun `cancel removes the partial file and leaves a retryable failure`() = runTest {
        val engine = FakeGemmaDownloadEngine()
        val files = files()
        val store = store(engine, files)
        val download = async { runCatching { store.install(GemmaModel.E2B) } }
        runCurrent()
        val destination = files.destination(GemmaModel.E2B)
        destination.writeBytes(ByteArray(8))
        store.cancel(GemmaModel.E2B)
        advanceUntilIdle()

        assertFalse(destination.exists())
        assertEquals(GemmaModelStatus.Failed, store.state(GemmaModel.E2B).status)
        assertEquals(1, engine.removed.size)
        assertTrue(download.await().isFailure)
    }

    @Test
    fun `refresh reconnects a near-complete file instead of marking it ready`() = runTest {
        val engine = FakeGemmaDownloadEngine()
        val ids = InMemoryGemmaDownloadIdStore()
        val files = files()
        val destination = files.destination(GemmaModel.E2B)
        destination.parentFile?.mkdirs()
        destination.writeBytes(ByteArray(12))
        val id = engine.enqueue(GemmaModel.E2B, destination, GemmaModel.E2B.name)
        ids.set(GemmaModel.E2B, id)
        engine.set(id, GemmaDownloadStatus.Running, 95, 100)

        val store = store(engine, files, ids)
        store.refresh()
        runCurrent()

        assertEquals(1, engine.enqueued.size)
        assertEquals(GemmaModelStatus.Downloading, store.state(GemmaModel.E2B).status)
        assertEquals(0.95f, store.state(GemmaModel.E2B).progress)
        assertEquals(id, ids.get(GemmaModel.E2B))

        val download = async { store.install(GemmaModel.E2B) }
        engine.succeed(id)
        download.await()
        assertEquals(GemmaModelStatus.Ready, store.state(GemmaModel.E2B).status)
        assertEquals(null, ids.get(GemmaModel.E2B))
    }

    @Test
    fun `refresh reconnects a persisted download without enqueueing again`() = runTest {
        val engine = FakeGemmaDownloadEngine()
        val ids = InMemoryGemmaDownloadIdStore()
        val files = files()
        val destination = files.destination(GemmaModel.E2B)
        val id = engine.enqueue(GemmaModel.E2B, destination, GemmaModel.E2B.name)
        ids.set(GemmaModel.E2B, id)
        engine.set(id, GemmaDownloadStatus.Running, 5, 10)

        val store = store(engine, files, ids)
        store.refresh()
        runCurrent()

        assertEquals(1, engine.enqueued.size)
        assertEquals(GemmaModelStatus.Downloading, store.state(GemmaModel.E2B).status)
        assertEquals(0.5f, store.state(GemmaModel.E2B).progress)

        val download = async { store.install(GemmaModel.E2B) }
        engine.succeed(id)
        download.await()
        assertEquals(GemmaModelStatus.Ready, store.state(GemmaModel.E2B).status)
    }

    @Test
    fun `delete removes an installed model and refuses a download in progress`() = runTest {
        val engine = FakeGemmaDownloadEngine()
        val files = files()
        files.destination(GemmaModel.E4B).writeBytes(ByteArray(32))
        val store = store(engine, files)
        store.refresh()
        store.delete(GemmaModel.E4B)

        assertFalse(files.destination(GemmaModel.E4B).exists())
        assertEquals(GemmaModelStatus.Missing, store.state(GemmaModel.E4B).status)

        val download = async { store.install(GemmaModel.E2B) }
        runCurrent()
        store.delete(GemmaModel.E2B)
        assertTrue(store.state(GemmaModel.E2B).isDownloading)
        engine.succeed(engine.enqueuedId(GemmaModel.E2B))
        download.await()
        assertEquals(GemmaModelStatus.Ready, store.state(GemmaModel.E2B).status)
    }

    private fun files() = GemmaModelFiles(directory) { 10 }

    private fun TestScope.store(
        engine: FakeGemmaDownloadEngine,
        files: GemmaModelFiles = files(),
        ids: GemmaDownloadIdStore = InMemoryGemmaDownloadIdStore(),
    ) = DefaultGemmaModelStore(engine, files, ids, backgroundScope)

    private fun GemmaModelStore.state(model: GemmaModel) =
        models.value.first { it.model == model }

    private fun FakeGemmaDownloadEngine.enqueuedId(model: GemmaModel): Long =
        idAt(enqueued.indexOf(model))

    private fun FakeGemmaDownloadEngine.lastId(model: GemmaModel): Long =
        idAt(enqueued.lastIndexOf(model))

    private fun idAt(index: Int): Long {
        check(index >= 0) { "not enqueued" }
        return index + 1L
    }
}
