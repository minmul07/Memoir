package minmul.memoir

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import minmul.memoir.data.preferences.InformationCollectionPreferencesStore
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/** Information collection preference snapshot loaded once at application startup. */
@Singleton
class InformationCollectionState @Inject constructor(
    private val preferences: InformationCollectionPreferencesStore,
) {
    private val _informationCollectionEnabled = MutableStateFlow<Boolean?>(null)
    val informationCollectionEnabled: StateFlow<Boolean?> =
        _informationCollectionEnabled.asStateFlow()

    suspend fun load() {
        try {
            val enabled = preferences.informationCollectionEnabled.first()
            _informationCollectionEnabled.value = enabled
            Timber.tag("MemoirStartup").i("information_collection_enabled=%s", enabled)
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            Timber.tag("MemoirStartup")
                .e(exception, "Failed to load information collection preference")
        }
    }
}
