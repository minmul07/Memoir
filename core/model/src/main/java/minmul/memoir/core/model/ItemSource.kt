package minmul.memoir.core.model

enum class ItemSource(val storedValue: String) {
    Picker("picker"),
    Share("share"),
    ;

    companion object {
        fun fromStored(value: String): ItemSource =
            entries.first { it.storedValue == value }
    }
}
