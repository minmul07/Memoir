package minmul.memoir.core.ai

import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.Conversation
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.Message
import com.google.ai.edge.litertlm.MessageCallback
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import io.mockk.verifyOrder
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import minmul.memoir.core.model.GemmaInferenceSettings
import minmul.memoir.core.model.GemmaModel
import minmul.memoir.core.model.LlmRuntimeStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class GemmaLlmEngineTest {
    // SDK classes are final JNI wrappers, so mock the native boundary only.
    private val native = mockk<Engine>(relaxed = true)
    private val conversation = mockk<Conversation>(relaxed = true)
    private val callback = slot<MessageCallback>()
    private val config = slot<ConversationConfig>()

    private fun engine(
        scope: TestScope,
        settings: suspend () -> GemmaInferenceSettings = { GemmaInferenceSettings() },
        setSpeculativeDecoding: (Boolean) -> Unit = {},
    ): GemmaLlmEngine {
        every { native.isInitialized() } returns true
        every { native.createConversation(capture(config)) } returns conversation
        every { conversation.sendMessageAsync(any<Contents>(), capture(callback)) } just Runs
        return GemmaLlmEngine(
            "cache",
            StandardTestDispatcher(scope.testScheduler),
            { native },
            settings,
            setSpeculativeDecoding,
        )
    }

    @Test
    fun `cancelled caller still releases engine and resets status`() = runTest {
        val engine = engine(this)
        engine.load(GemmaModel.E2B, File("model"))
        val task = launch {
            try {
                awaitCancellation()
            } finally {
                engine.close()
            }
        }
        runCurrent()
        task.cancelAndJoin()
        verify(exactly = 1) { native.close() }
        assertEquals(LlmRuntimeStatus.Idle, engine.status.value)
    }

    @Test
    fun `cancellation at initialization return still releases loaded engine`() = runTest {
        val engine = engine(this)
        lateinit var task: Job
        every { native.initialize() } answers { task.cancel() }
        task = launch {
            try {
                engine.load(GemmaModel.E2B, File("model"))
            } finally {
                engine.close()
            }
        }
        task.join()
        verify(exactly = 1) { native.close() }
        assertEquals(LlmRuntimeStatus.Idle, engine.status.value)
    }

    @Test
    fun `inference cancellation waits for native completion before releasing resources`() =
        runTest {
            val engine = engine(this)
            engine.load(GemmaModel.E2B, File("model"))
            val task = launch {
                try {
                    engine.summarize("/image", "text")
                } finally {
                    engine.close()
                }
            }
            runCurrent()
            task.cancel()
            runCurrent()
            verify(exactly = 1) { conversation.cancelProcess() }
            verify(exactly = 0) { conversation.close(); native.close() }
            assertFalse(task.isCompleted)

            callback.captured.onError(CancellationException("cancelled"))
            task.join()
            verifyOrder { conversation.cancelProcess(); conversation.close(); native.close() }
            assertEquals(LlmRuntimeStatus.Idle, engine.status.value)
        }

    @Test
    fun `successful inference joins chunks and closes conversation without cancellation`() =
        runTest {
            val engine = engine(this)
            engine.load(GemmaModel.E2B, File("model"))
            val task = async { engine.summarize("/image", null) }
            runCurrent()
            for (text in listOf("first", " second")) {
                val message = mockk<Message>()
                every { message.toString() } returns text
                callback.captured.onMessage(message)
            }
            callback.captured.onDone()
            assertEquals("first second", task.await())
            verify(exactly = 1) { conversation.close() }
            verify(exactly = 0) { conversation.cancelProcess() }
            engine.close()
        }

    @Test
    fun `summarize passes stored conversation settings`() = runTest {
        val engine = engine(
            this,
            settings = {
                GemmaInferenceSettings(
                    maxOutputToken = 2048,
                    topK = 16,
                    thinkingEnabled = true,
                    topP = 0.8,
                    temperature = 0.5,
                )
            },
        )
        engine.load(GemmaModel.E2B, File("model"))
        val task = async { engine.summarize("/image", null) }
        runCurrent()
        callback.captured.onDone()
        task.await()
        assertEquals(2048, config.captured.maxOutputToken)
        assertEquals(16, config.captured.samplerConfig?.topK)
        assertEquals(0.8, config.captured.samplerConfig?.topP)
        assertEquals(0.5, config.captured.samplerConfig?.temperature)
        assertEquals(true, config.captured.thinkingConfig?.enableThinking)
        engine.close()
    }

    @Test
    fun `load applies speculative decoding before initialize`() = runTest {
        val flags = mutableListOf<Boolean>()
        val engine = engine(
            this,
            settings = { GemmaInferenceSettings(speculativeDecodingEnabled = false) },
            setSpeculativeDecoding = { flags += it },
        )
        every { native.initialize() } answers {
            assertEquals(listOf(false), flags)
        }
        engine.load(GemmaModel.E2B, File("model"))
        assertEquals(listOf(false), flags)
        verify(exactly = 1) { native.initialize() }
        engine.close()
    }
}
