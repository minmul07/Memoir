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
        complete(imagePath, prompt(ocrText))

    override suspend fun extract(imagePath: String, ocrText: String?): String =
        complete(imagePath, extractPrompt(ocrText))

    private suspend fun complete(imagePath: String, promptText: String): String =
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
                            Content.Text(promptText),
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
        "$SYSTEM_PROMPT_1_PASS\n\nOCR:\n${ocrText.orEmpty()}"

    private fun extractPrompt(ocrText: String?): String =
        "$SYSTEM_PROMPT_2_PASS\n\nOCR:\n${ocrText.orEmpty()}"

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
        const val SYSTEM_PROMPT_1_PASS =
            """이미지와 OCR을 읽고 한국어로 답하세요.

반드시 아래 순서를 따르세요.

첫 줄: 구체적인 제목 하나.
다음 줄부터: 핵심 내용을 구체적으로 정리. 마크다운 문법 사용 가능."""

        const val SYSTEM_PROMPT_2_PASS =
            """스크린샷의 핵심 특수 정보만 추출하세요. 제목과 설명문은 다른 단계에서 작성하므로 여기에는 넣지 않습니다.

다음 조건을 모두 만족하는 사실만 남기세요.
1. 화면의 주된 일정, 구매, 결제, 송금, 방문 또는 연락에 직접 관련된다.
2. 날짜·시각·기간, 실제 가격·거래금액, 방문 장소, 계좌·예약·연락 번호처럼 따로 복사하거나 확인할 구체적인 값이다.
3. 이미지나 OCR에 실제 값과 그 의미가 확인된다.

주된 일정의 시작·종료·마감, 실제 판매가·결제금액, 거래일시, 계좌번호는 누락하지 마세요. 0원도 실제 금액입니다. 이름·브랜드·상품 설명·상태·수량·평점·리뷰·제품 사양·적립 및 할부 광고·일반 약관은 본문에 맡기세요. 상태표시줄과 사업자 푸터도 제외합니다.
계좌번호와 카드번호를 혼동하지 마세요. 일정과 무관한 구매일·승인번호를 모두 나열하지 마세요. 현재 가격과 취소선 가격을 구분하세요.
여러 대상의 일시는 대상이 드러나게 이름을 붙입니다. 같은 사실은 한 번만 넣으세요. 핵심 값이 있는 화면에서는 이를 추출하고, 실제로 없을 때만 `NULL`을 출력하세요.
항목명은 의미가 분명한 짧은 한국어로 자유롭게 정하세요. 값은 원문의 숫자·단위·날짜를 보존한 짧은 문자열입니다. 빈 값, 추측, 예시 값은 넣지 마세요. 각 줄로 구분된 `name: value` 쌍으로 결과만 반환하세요.


출력은 각 줄마다 `name: value`입니다. name에는 속성명, value에는 그 실제 값을 넣으세요. `name`이나 `value`를 키로 사용하지 마세요. 예를 들어 "납부기한: 12월 5일"처럼 작성합니다. 이 예시의 값은 복사하지 마세요. 항목은 가장 중요한 것부터 최대 5개만 출력하세요. 추출할 정보가 없으면 `NULL`을 출력하세요."""
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
