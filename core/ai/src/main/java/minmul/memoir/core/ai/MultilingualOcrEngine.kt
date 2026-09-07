package minmul.memoir.core.ai

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import minmul.memoir.core.model.OcrModel
import java.io.File
import java.util.Locale

class MultilingualOcrEngine(
    private val context: Context,
    private val enabledModels: suspend () -> Set<OcrModel>,
) : OcrEngine {
    private val client = MlKitOcrClient(context)
    val modelManager: OcrModelManager = DefaultOcrModelManager(
        client,
        CoroutineScope(SupervisorJob() + Dispatchers.IO),
        AnalysisLog::write,
    )

    override suspend fun recognize(imagePath: String): String? {
        val locales = context.resources.configuration.locales
        val locale = if (locales.isEmpty) Locale.getDefault() else locales[0]
        val defaultModel = defaultOcrModel(locale)
        val models = modelsForRecognition(modelManager, defaultModel, enabledModels())
        AnalysisLog.write("ocr available=$models default=$defaultModel")
        if (models.isEmpty()) {
            AnalysisLog.write("ocr skipped no_enabled_models")
            return null
        }
        return withContext(Dispatchers.IO) {
            val image =
                InputImage.fromFilePath(context, Uri.fromFile(File(context.filesDir, imagePath)))
            val candidates = models.map { model ->
                currentCoroutineContext().ensureActive()
                val started = System.nanoTime()
                AnalysisLog.write("ocr model=$model begin")
                val result = try {
                    client.recognizers.getValue(model).process(image).awaitCompletion()
                } catch (error: Exception) {
                    AnalysisLog.write("ocr model=$model error=${error.javaClass.simpleName}")
                    throw error
                }
                currentCoroutineContext().ensureActive()
                OcrCandidate(
                    text = result.text,
                    lineConfidences = result.textBlocks.flatMap { it.lines }
                        .filter { it.text.isNotBlank() }
                        .map { it.confidence },
                ).also {
                    AnalysisLog.write("ocr model=$model complete elapsedMs=${(System.nanoTime() - started) / 1_000_000} lines=${it.lineConfidences.size} chars=${it.text.length} meanConfidence=${it.averageConfidence}")
                }
            }
            val winner =
                candidates.filter { it.text.isNotBlank() }.maxByOrNull { it.averageConfidence }
            val selected = candidates.indexOfFirst { it === winner }
            AnalysisLog.write("ocr selected=${models.getOrNull(selected) ?: "none"} meanConfidence=${winner?.averageConfidence} chars=${winner?.text?.length ?: 0}")
            selectOcrText(candidates)
        }
    }
}
