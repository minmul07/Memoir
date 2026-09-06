package minmul.memoir.core.model

object OcrText {
    fun stored(value: String?): String? = value?.takeIf { it.isNotBlank() }
}
