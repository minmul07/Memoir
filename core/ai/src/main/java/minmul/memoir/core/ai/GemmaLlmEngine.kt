package minmul.memoir.core.ai

import android.content.Context
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.BenchmarkInfo
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.Conversation
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
import java.util.Locale

class GemmaLlmEngine internal constructor(
    private val cacheDir: String,
    private val ioDispatcher: CoroutineDispatcher,
    private val createEngine: (EngineConfig) -> Engine,
    private val settings: suspend () -> GemmaInferenceSettings = { GemmaInferenceSettings() },
    private val setSpeculativeDecoding: (Boolean) -> Unit = ::applySpeculativeDecodingFlag,
    private val setBenchmark: (Boolean) -> Unit = ::applyBenchmarkFlag,
    private val benchmarkInfo: (Conversation) -> BenchmarkInfo? = ::readBenchmarkInfo,
    private val log: (String) -> Unit = AnalysisLog::write,
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
            setBenchmark(true)
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
                    val started = System.nanoTime()
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
                    val text = try {
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
                    logInference(started, conversation)
                    text
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

    private fun logInference(startedNs: Long, conversation: Conversation) {
        runCatching {
            val inferenceMs = (System.nanoTime() - startedNs) / 1_000_000
            log(formatInferenceLog(inferenceMs, benchmarkInfo(conversation)))
        }
    }

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
            """당신은 스크린샷 분석기다. 한국어로 답한다.

반드시 다음 형식으로 출력한다.

title: 짧은 제목
---
스크린샷의 중요한 정보를 빠짐없이 정리한 본문.
---
특수 정보가 있으면 한 줄에 하나씩 출력한다.
없으면 출력하지 않는다.

허용 형식:
time(name): value
period(name): value
location(name): value
account(name): value
phone(name): value

규칙:
- name은 반드시 작성한다.
- 원문에 없는 정보는 추측하지 않는다.
- 같은 정보를 중복하지 않는다.
- 이미지와 OCR을 함께 사용한다.
- OCR이 비어 있으면 이미지만 사용한다.
- 특수 정보는 핵심 정보일 경우에만 사용한다."""
    }
}

@OptIn(ExperimentalApi::class)
private fun applySpeculativeDecodingFlag(enabled: Boolean) {
    ExperimentalFlags.enableSpeculativeDecoding = enabled
}

@OptIn(ExperimentalApi::class)
private fun applyBenchmarkFlag(enabled: Boolean) {
    ExperimentalFlags.enableBenchmark = enabled
}

@OptIn(ExperimentalApi::class)
private fun readBenchmarkInfo(conversation: Conversation): BenchmarkInfo? =
    runCatching { conversation.getBenchmarkInfo() }.getOrNull()

private fun formatInferenceLog(inferenceMs: Long, stats: BenchmarkInfo?): String {
    if (stats == null) return "gemma infer complete inferenceMs=$inferenceMs"
    return "gemma infer complete " +
            "ttftMs=${(stats.timeToFirstTokenInSecond * 1_000).toLong()}ms " +
            "inferenceMs=${inferenceMs}ms " +
            "inputRate=${"%.2f".format(Locale.US, stats.lastPrefillTokensPerSecond)}tk/s " +
            "outputRate=${"%.2f".format(Locale.US, stats.lastDecodeTokensPerSecond)}tk/s " +
            "inputTokens=${stats.lastPrefillTokenCount}tks " +
            "outputTokens=${stats.lastDecodeTokenCount}tks"
}
