package minmul.memoir.background.analysis

import minmul.memoir.core.model.GemmaModel
import java.io.File

data class LocatedGemmaModel(val model: GemmaModel, val file: File)

fun interface ReadyGemmaModelLocator {
    suspend fun locate(): LocatedGemmaModel?
}
