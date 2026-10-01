package minmul.memoir.feature.settings

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import minmul.memoir.data.preferences.InformationCollectionPreferencesStore

class FakeInformationCollectionPreferencesStore(
    enabled: Boolean = true,
) : InformationCollectionPreferencesStore {
    val savedEnabled = MutableStateFlow(enabled)
    override var informationCollectionEnabled: Flow<Boolean> = savedEnabled
    var failWrites = false
    var writeGate: CompletableDeferred<Unit>? = null
    var writeCount = 0

    override suspend fun setInformationCollectionEnabled(enabled: Boolean) {
        writeCount++
        writeGate?.await()
        check(!failWrites) { "write_failed" }
        savedEnabled.value = enabled
    }
}
