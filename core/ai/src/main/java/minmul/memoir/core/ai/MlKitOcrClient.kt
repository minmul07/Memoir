package minmul.memoir.core.ai

import android.content.Context
import com.google.android.gms.common.moduleinstall.InstallStatusListener
import com.google.android.gms.common.moduleinstall.ModuleInstall
import com.google.android.gms.common.moduleinstall.ModuleInstallRequest
import com.google.android.gms.common.moduleinstall.ModuleInstallStatusUpdate
import com.google.android.gms.common.moduleinstall.ModuleInstallStatusUpdate.InstallState.STATE_CANCELED
import com.google.android.gms.common.moduleinstall.ModuleInstallStatusUpdate.InstallState.STATE_COMPLETED
import com.google.android.gms.common.moduleinstall.ModuleInstallStatusUpdate.InstallState.STATE_DOWNLOADING
import com.google.android.gms.common.moduleinstall.ModuleInstallStatusUpdate.InstallState.STATE_DOWNLOAD_PAUSED
import com.google.android.gms.common.moduleinstall.ModuleInstallStatusUpdate.InstallState.STATE_FAILED
import com.google.android.gms.common.moduleinstall.ModuleInstallStatusUpdate.InstallState.STATE_INSTALLING
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import com.google.mlkit.vision.text.devanagari.DevanagariTextRecognizerOptions
import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import minmul.memoir.core.model.OcrModel
import minmul.memoir.core.model.OcrModelState
import minmul.memoir.core.model.OcrModelStatus
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.time.Duration.Companion.milliseconds

internal class MlKitOcrClient(context: Context) : OcrModelBackend {
    val recognizers = mapOf(
        OcrModel.Korean to TextRecognition.getClient(KoreanTextRecognizerOptions.Builder().build()),
        OcrModel.Japanese to TextRecognition.getClient(
            JapaneseTextRecognizerOptions.Builder().build()
        ),
        OcrModel.Chinese to TextRecognition.getClient(
            ChineseTextRecognizerOptions.Builder().build()
        ),
        OcrModel.Latin to TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS),
        OcrModel.Devanagari to TextRecognition.getClient(
            DevanagariTextRecognizerOptions.Builder().build()
        ),
    )
    private val modules = ModuleInstall.getClient(context)

    override suspend fun isAvailable(model: OcrModel): Boolean =
        modules.areModulesAvailable(recognizers.getValue(model)).await().areModulesAvailable()

    override suspend fun install(model: OcrModel, onProgress: (OcrModelState) -> Unit) {
        // One listener per request; buffer events that arrive before installModules returns.
        val updates = Channel<ModuleInstallStatusUpdate>(Channel.UNLIMITED)
        val listener = InstallStatusListener { updates.trySend(it) }
        try {
            withTimeout(120_000.milliseconds) {
                val response = modules.installModules(
                    ModuleInstallRequest.newBuilder()
                        .addApi(recognizers.getValue(model))
                        .setListener(listener)
                        .build(),
                ).awaitCompletion()
                if (!response.areModulesAlreadyInstalled()) {
                    while (true) {
                        val update = updates.receive()
                        when (update.installState) {
                            STATE_COMPLETED -> break
                            STATE_FAILED, STATE_CANCELED ->
                                error("ocr_model_install_failed_${update.errorCode}")

                            else -> {
                                val status = when (update.installState) {
                                    STATE_DOWNLOADING -> OcrModelStatus.Downloading
                                    STATE_DOWNLOAD_PAUSED -> OcrModelStatus.Paused
                                    STATE_INSTALLING -> OcrModelStatus.Installing
                                    else -> OcrModelStatus.Pending
                                }
                                val progress = update.progressInfo
                                onProgress(
                                    OcrModelState(
                                        model = model,
                                        status = status,
                                        downloadedBytes = progress?.bytesDownloaded ?: 0,
                                        totalBytes = progress?.totalBytesToDownload,
                                    )
                                )
                            }
                        }
                    }
                }
            }
        } catch (timeout: TimeoutCancellationException) {
            currentCoroutineContext().ensureActive()
            throw IllegalStateException("ocr_model_install_timeout", timeout)
        } finally {
            withContext(NonCancellable) {
                try {
                    withTimeout(5_000.milliseconds) { modules.unregisterListener(listener).await() }
                } catch (error: Exception) {
                    AnalysisLog.write("models model=$model listener_cleanup error=${error.javaClass.simpleName}")
                }
            }
            updates.close()
        }
    }
}

// ML Kit cannot cancel native work. Wait until it finishes before processing another model/image.
internal suspend fun <T> Task<T>.awaitCompletion(): T =
    suspendCancellableCoroutine { continuation ->
        addOnCompleteListener { task ->
            if (task.isSuccessful) continuation.resume(task.result)
            else continuation.resumeWithException(
                task.exception ?: IllegalStateException("ocr_failed")
            )
        }
    }
