package minmul.memoir.data.model

import minmul.memoir.core.model.GemmaModel
import java.io.File

class GemmaModelFiles(
    private val modelsDir: File,
    private val minBytesFor: (GemmaModel) -> Long = { it.minBytes },
) {
    fun destination(model: GemmaModel): File = File(modelsDir, model.fileName)

    fun isReady(model: GemmaModel): Boolean {
        val file = destination(model)
        return file.isFile && file.length() >= minBytesFor(model)
    }

    fun delete(model: GemmaModel) {
        destination(model).delete()
    }

    fun installedFile(model: GemmaModel): File? = destination(model).takeIf { isReady(model) }

    fun ensureDirectory() {
        if (!modelsDir.exists()) modelsDir.mkdirs()
    }
}
