package minmul.memoir.core.model

sealed interface LlmRuntimeStatus {
    data object Idle : LlmRuntimeStatus
    data object Missing : LlmRuntimeStatus
    data class Loading(val model: GemmaModel) : LlmRuntimeStatus
    data class Ready(val model: GemmaModel) : LlmRuntimeStatus
    data class Failed(val model: GemmaModel, val errorClass: String) : LlmRuntimeStatus
}

fun preferredReadyGemmaModel(
    selected: GemmaModel?,
    ready: Collection<GemmaModel>,
): GemmaModel? {
    val readySet = ready.toSet()
    return selected?.takeIf { it in readySet } ?: readySet.minByOrNull { it.expectedBytes }
}
