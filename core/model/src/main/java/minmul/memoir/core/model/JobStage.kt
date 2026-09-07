package minmul.memoir.core.model

enum class JobStage(val storedValue: String) {
    Waiting("waiting"),
    Ocr("ocr"),
    Infer("infer"),
    Saving("saving"),
    ;

    companion object {
        fun fromStored(value: String): JobStage =
            entries.first { it.storedValue == value }
    }
}
