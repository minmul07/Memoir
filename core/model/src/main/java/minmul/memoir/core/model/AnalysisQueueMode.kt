package minmul.memoir.core.model

enum class AnalysisQueueMode {
    Manual,
    Scheduled,
    Immediate,
    ;

    val startsAutomatically: Boolean
        get() = this == Immediate
}
