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
        "$SYSTEM_PROMPT\n\nOCR:\n${ocrText.orEmpty()}"

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
        const val SYSTEM_PROMPT =
            "당신은 스크린샷 분석기다. 한국어로 답한다.\n" +
                    "아래 형식으로만 출력한다. 코드펜스나 설명 문장을 쓰지 않는다.\n" +
                    "title: 짧은 제목\n" +
                    "summary: 한 줄 요약\n" +
                    "---\n" +
                    "스크린샷에 포함된 정보를 빠짐없이 정리한 본문. 복잡해도 축약하지 않는다. 필요한 경우 마크다운을 사용할 수 있다.\n" +
                    "summary 줄은 없으면 생략한다. 이미지와 OCR을 함께 사용한다. OCR이 비어 있으면 이미지만으로 작성한다."
    }
}

@OptIn(ExperimentalApi::class)
private fun applySpeculativeDecodingFlag(enabled: Boolean) {
    ExperimentalFlags.enableSpeculativeDecoding = enabled
}
