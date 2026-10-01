package minmul.memoir

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import minmul.memoir.data.preferences.InformationCollectionPreferencesStore

class FakeInformationCollectionPreferencesStore(
    enabled: Boolean = true,
) : InformationCollectionPreferencesStore {
    private val state = MutableStateFlow(enabled)
    override var informationCollectionEnabled: Flow<Boolean> = state

    override suspend fun setInformationCollectionEnabled(enabled: Boolean) {
        state.value = enabled
    }
}
