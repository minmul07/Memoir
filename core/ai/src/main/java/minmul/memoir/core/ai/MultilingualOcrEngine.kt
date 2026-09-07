package minmul.memoir.core.ai

import android.content.Context
import android.net.Uri
import com.google.android.gms.common.moduleinstall.ModuleInstall
import com.google.android.gms.common.moduleinstall.ModuleInstallRequest
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import com.google.mlkit.vision.text.devanagari.DevanagariTextRecognizerOptions
import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.time.Duration.Companion.milliseconds

class MultilingualOcrEngine(private val context: Context) : OcrEngine, OcrModelManager {
    private val recognizers = listOf(
        TextRecognition.getClient(KoreanTextRecognizerOptions.Builder().build()),
        TextRecognition.getClient(JapaneseTextRecognizerOptions.Builder().build()),
        TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build()),
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS),
        TextRecognition.getClient(DevanagariTextRecognizerOptions.Builder().build()),
    )
    private val modelNames = listOf("korean", "japanese", "chinese", "latin", "devanagari")
    private val modules = ModuleInstall.getClient(context)
    private val installLock = Mutex()

    override suspend fun isAvailable(): Boolean {
        val available =
            modules.areModulesAvailable(*recognizers.toTypedArray()).await().areModulesAvailable()
        AnalysisLog.write("models available=$available count=${recognizers.size}")
        return available
    }

    override suspend fun install() = installLock.withLock {
        if (!isAvailable()) {
            AnalysisLog.write("models download_begin")
            modules.installModules(
                ModuleInstallRequest.newBuilder().apply { recognizers.forEach { addApi(it) } }
                    .build()
            ).await()
            // installModules returns a session before the model download completes.
            // Availability, not session creation, gates OCR.
            withTimeout(120_000.milliseconds) {
                while (!isAvailable()) delay(500.milliseconds)
            }
            AnalysisLog.write("models download_complete")
        }
    }

    override suspend fun recognize(imagePath: String): String? {
        install()
        return withContext(Dispatchers.IO) {
            val image =
                InputImage.fromFilePath(context, Uri.fromFile(File(context.filesDir, imagePath)))
            // ML Kit tasks cannot cancel inference. Wait for native processing to finish
            // before the sequential runner starts another image.
            val candidates = recognizers.mapIndexed { index, recognizer ->
                currentCoroutineContext().ensureActive()
                val started = System.nanoTime()
                AnalysisLog.write("ocr model=${modelNames[index]} begin")
                val result = try {
                    recognizer.process(image).awaitCompletion()
                } catch (error: Exception) {
                    AnalysisLog.write("ocr model=${modelNames[index]} error=${error.javaClass.simpleName}")
                    throw error
                }
                currentCoroutineContext().ensureActive()
                OcrCandidate(
                    text = result.text,
                    lineConfidences = result.textBlocks.flatMap { it.lines }
                        .filter { it.text.isNotBlank() }
                        .map { it.confidence },
                ).also {
                    AnalysisLog.write("ocr model=${modelNames[index]} complete elapsedMs=${(System.nanoTime() - started) / 1_000_000} lines=${it.lineConfidences.size} chars=${it.text.length} meanConfidence=${it.averageConfidence}")
                }
            }
            val winner =
                candidates.filter { it.text.isNotBlank() }.maxByOrNull { it.averageConfidence }
            val selected = candidates.indexOfFirst { it === winner }
            AnalysisLog.write("ocr selected=${modelNames.getOrNull(selected) ?: "none"} meanConfidence=${winner?.averageConfidence} chars=${winner?.text?.length ?: 0}")
            selectOcrText(candidates)
        }
    }
}

private suspend fun <T> Task<T>.awaitCompletion(): T = suspendCancellableCoroutine { continuation ->
    addOnCompleteListener { task ->
        if (task.isSuccessful) continuation.resume(task.result)
        else continuation.resumeWithException(task.exception ?: IllegalStateException("ocr_failed"))
    }
}
