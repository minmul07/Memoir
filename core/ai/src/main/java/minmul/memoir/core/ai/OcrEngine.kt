package minmul.memoir.core.ai

interface OcrEngine {
    suspend fun recognize(imagePath: String): String?
}

interface OcrModelManager {
    suspend fun isAvailable(): Boolean
    suspend fun install()
}
