package minmul.memoir.background.analysis

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import minmul.memoir.core.ai.LlmEngine
import minmul.memoir.core.model.GemmaModel
import minmul.memoir.core.model.LlmRuntimeStatus
import java.io.File

class FakeLlmEngine(
    private val loadError: Throwable? = null,
    private val onSummarize: suspend (imagePath: String, ocrText: String?) -> String = { _, _ -> VALID_PAYLOAD },
) : LlmEngine {
    private val mutableStatus = MutableStateFlow<LlmRuntimeStatus>(LlmRuntimeStatus.Idle)
    override val status = mutableStatus.asStateFlow()
    var loadCalls = 0
    var closeCalls = 0
    val summarizeCalls = mutableListOf<Pair<String, String?>>()

    override fun markIdle() {
        mutableStatus.value = LlmRuntimeStatus.Idle
    }

    override fun markMissing() {
        mutableStatus.value = LlmRuntimeStatus.Missing
    }

    override suspend fun load(model: GemmaModel, file: File) {
        loadCalls++
        loadError?.let {
            mutableStatus.value = LlmRuntimeStatus.Failed(model, it.javaClass.simpleName)
            throw it
        }
        mutableStatus.value = LlmRuntimeStatus.Ready(model)
    }

    override suspend fun summarize(imagePath: String, ocrText: String?): String {
        summarizeCalls += imagePath to ocrText
        return onSummarize(imagePath, ocrText)
    }

    override suspend fun close() {
        closeCalls++
        if (mutableStatus.value is LlmRuntimeStatus.Ready) {
            mutableStatus.value = LlmRuntimeStatus.Idle
        }
    }
}

const val VALID_PAYLOAD = "title: T\n---\nD"
