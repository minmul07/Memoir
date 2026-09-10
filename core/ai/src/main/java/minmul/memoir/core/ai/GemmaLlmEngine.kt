package minmul.memoir.core.ai

import android.content.Context
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.ExperimentalApi
import com.google.ai.edge.litertlm.ExperimentalFlags
import com.google.ai.edge.litertlm.Message
import com.google.ai.edge.litertlm.MessageCallback
import com.google.ai.edge.litertlm.SamplerConfig
import com.google.ai.edge.litertlm.ThinkingConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import minmul.memoir.core.model.GemmaInferenceSettings
import minmul.memoir.core.model.GemmaModel
import minmul.memoir.core.model.LlmRuntimeStatus
import java.io.File

class GemmaLlmEngine internal constructor(
    private val cacheDir: String,
    private val ioDispatcher: CoroutineDispatcher,
    private val createEngine: (EngineConfig) -> Engine,
    private val settings: suspend () -> GemmaInferenceSettings = { GemmaInferenceSettings() },
    private val setSpeculativeDecoding: (Boolean) -> Unit = ::applySpeculativeDecodingFlag,
) : LlmEngine {
    constructor(
        context: Context,
        ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
        settings: suspend () -> GemmaInferenceSettings = { GemmaInferenceSettings() },
    ) : this(context.applicationContext.cacheDir.path, ioDispatcher, ::Engine, settings)

    private val mutex = Mutex()
    private val mutableStatus = MutableStateFlow<LlmRuntimeStatus>(LlmRuntimeStatus.Idle)
    private var engine: Engine? = null

    override val status = mutableStatus.asStateFlow()

    override fun markIdle() {
        mutableStatus.value = LlmRuntimeStatus.Idle
    }

    override fun markMissing() {
        mutableStatus.value = LlmRuntimeStatus.Missing
    }

    override suspend fun load(model: GemmaModel, file: File) = withContext(ioDispatcher) {
        mutex.withLock {
            releaseNative()
            mutableStatus.value = LlmRuntimeStatus.Loading(model)
            val inference = settings()
            setSpeculativeDecoding(inference.speculativeDecodingEnabled)
            val created = createEngine(
                EngineConfig(
                    modelPath = file.absolutePath,
                    backend = Backend.GPU(),
                    visionBackend = Backend.GPU(),
                    maxNumImages = 1,
                    cacheDir = cacheDir,
                ),
            )
            try {
                created.initialize()
                engine = created
                mutableStatus.value = LlmRuntimeStatus.Ready(model)
            } catch (cancelled: CancellationException) {
                releaseCreated(created)
                mutableStatus.value = LlmRuntimeStatus.Idle
                throw cancelled
            } catch (error: Exception) {
                releaseCreated(created)
                mutableStatus.value = LlmRuntimeStatus.Failed(model, error.javaClass.simpleName)
                throw error
            }
        }
    }

    override suspend fun summarize(imagePath: String, ocrText: String?): String =
        withContext(ioDispatcher) {
            val inference = settings()
            mutex.withLock {
                val current = checkNotNull(engine) { "engine_not_ready" }
                current.createConversation(conversationConfig(inference)).use { conversation ->
                    val response = StringBuffer()
                    val finished = CompletableDeferred<String>()
                    conversation.sendMessageAsync(
                        Contents.of(
                            Content.ImageFile(imagePath),
                            Content.Text(prompt(ocrText)),
                        ),
                        object : MessageCallback {
                            override fun onMessage(message: Message) {
                                response.append(message.toString())
                            }

                            override fun onDone() {
                                finished.complete(response.toString())
                            }

                            override fun onError(throwable: Throwable) {
                                finished.completeExceptionally(throwable)
                            }
                        },
                    )
                    try {
                        finished.await()
                    } catch (cancelled: CancellationException) {
                        // Kotlin cancellation does not stop native inference. Wait for its
                        // terminal callback before closing the conversation and engine.
                        withContext(NonCancellable) {
                            if (!finished.isCompleted) conversation.cancelProcess()
                            runCatching { finished.await() }
                        }
                        throw cancelled
                    }
                }
            }
        }

    override suspend fun close() = withContext(NonCancellable + ioDispatcher) {
        mutex.withLock {
            releaseNative()
            if (mutableStatus.value is LlmRuntimeStatus.Ready) {
                mutableStatus.value = LlmRuntimeStatus.Idle
            }
        }
    }

    private fun releaseNative() {
        val current = engine ?: return
        engine = null
        if (current.isInitialized()) current.close()
    }

    private fun releaseCreated(created: Engine) {
        if (created.isInitialized()) created.close()
        if (engine === created) engine = null
    }

    private fun prompt(ocrText: String?): String =
        "$SUMMARY_PROMPT\n${ocrText.orEmpty()}"

    private fun conversationConfig(settings: GemmaInferenceSettings) = ConversationConfig(
        samplerConfig = SamplerConfig(
            topK = settings.topK,
            topP = settings.topP,
            temperature = settings.temperature,
        ),
        thinkingConfig = ThinkingConfig(enableThinking = settings.thinkingEnabled),
        maxOutputToken = settings.maxOutputToken,
    )

    private companion object {
        const val SUMMARY_PROMPT = "이 이미지와 아래 OCR 텍스트를 한 문단으로 요약하세요."
    }
}

@OptIn(ExperimentalApi::class)
private fun applySpeculativeDecodingFlag(enabled: Boolean) {
    ExperimentalFlags.enableSpeculativeDecoding = enabled
}
