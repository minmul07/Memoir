package minmul.memoir.core.ai

import android.content.Context
import android.net.Uri
import com.google.android.gms.common.moduleinstall.ModuleInstall
import com.google.android.gms.common.moduleinstall.ModuleInstallRequest
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class KoreanOcrEngine(private val context: Context) : OcrEngine, OcrModelManager {
    private val recognizer = TextRecognition.getClient(KoreanTextRecognizerOptions.Builder().build())
    private val modules = ModuleInstall.getClient(context)
    private val installLock = Mutex()

    override suspend fun isAvailable() = modules.areModulesAvailable(recognizer).await().areModulesAvailable()

    override suspend fun install() = installLock.withLock {
        if (!isAvailable()) {
            modules.installModules(ModuleInstallRequest.newBuilder().addApi(recognizer).build()).await()
            // installModules returns a session before the model download completes.
            // Availability, not session creation, gates OCR.
            withTimeout(120_000) {
                while (!isAvailable()) delay(500)
            }
        }
    }

    override suspend fun recognize(imagePath: String): String? {
        install()
        return withContext(Dispatchers.IO) {
            val image = InputImage.fromFilePath(context, Uri.fromFile(File(context.filesDir, imagePath)))
            // ML Kit tasks cannot cancel inference. Wait for native processing to finish
            // before the sequential runner starts another image.
            val result = recognizer.process(image).awaitCompletion()
            currentCoroutineContext().ensureActive()
            result.text.takeIf { it.isNotBlank() }
        }
    }
}

private suspend fun <T> Task<T>.awaitCompletion(): T = suspendCoroutine { continuation ->
    addOnCompleteListener { task ->
        if (task.isSuccessful) continuation.resume(task.result)
        else continuation.resumeWithException(task.exception ?: IllegalStateException("ocr_failed"))
    }
}
