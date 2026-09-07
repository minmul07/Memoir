package minmul.memoir.core.ai

import minmul.memoir.core.model.OcrModel
import minmul.memoir.core.model.OcrModelStatus
import java.util.Locale

internal fun defaultOcrModel(locale: Locale): OcrModel = when (locale.script) {
    "Kore", "Hang" -> OcrModel.Korean
    "Jpan", "Hira", "Kana" -> OcrModel.Japanese
    "Hans", "Hant", "Hani" -> OcrModel.Chinese
    "Deva" -> OcrModel.Devanagari
    "Latn" -> OcrModel.Latin
    "" -> when (locale.language) {
        "ko" -> OcrModel.Korean
        "ja" -> OcrModel.Japanese
        "zh", "yue" -> OcrModel.Chinese
        "hi", "mr", "ne", "sa" -> OcrModel.Devanagari
        else -> OcrModel.Latin
    }

    else -> OcrModel.Latin
}

internal suspend fun modelsForRecognition(
    manager: OcrModelManager,
    defaultModel: OcrModel,
    enabledModels: Set<OcrModel> = OcrModel.entries.toSet(),
): List<OcrModel> {
    if (enabledModels.isEmpty()) return emptyList()
    val states = manager.refresh()
    val available = states.filter { it.status == OcrModelStatus.Ready }.map { it.model }
    if (available.isNotEmpty()) return available.filter { it in enabledModels }
    // Failed availability checks are not evidence that the device has no models.
    check(states.none { it.status == OcrModelStatus.Failed }) { "ocr_model_check_failed" }
    val fallback = defaultModel.takeIf { it in enabledModels }
        ?: OcrModel.entries.first { it in enabledModels }
    manager.install(fallback)
    return listOf(fallback)
}
