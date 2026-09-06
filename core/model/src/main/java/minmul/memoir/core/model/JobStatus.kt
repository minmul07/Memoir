package minmul.memoir.core.model

enum class JobStatus(val storedValue: String) {
    Queued("queued"),
    Running("running"),
    Succeeded("succeeded"),
    Failed("failed"),
    Cancelled("cancelled"),
    ;

    companion object {
        fun fromStored(value: String): JobStatus =
            entries.first { it.storedValue == value }
    }
}
