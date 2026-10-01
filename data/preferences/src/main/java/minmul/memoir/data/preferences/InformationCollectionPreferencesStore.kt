package minmul.memoir.data.preferences

import kotlinx.coroutines.flow.Flow

interface InformationCollectionPreferencesStore {
    val informationCollectionEnabled: Flow<Boolean>

    suspend fun setInformationCollectionEnabled(enabled: Boolean)
}
