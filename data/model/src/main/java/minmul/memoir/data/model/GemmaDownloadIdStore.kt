package minmul.memoir.data.model

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import minmul.memoir.core.model.GemmaModel

val Context.gemmaDownloadsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "gemma_downloads",
)

class DataStoreGemmaDownloadIdStore(
    private val dataStore: DataStore<Preferences>,
) : GemmaDownloadIdStore {
    override suspend fun get(model: GemmaModel): Long? = dataStore.data.first()[key(model)]

    override suspend fun set(model: GemmaModel, id: Long) {
        dataStore.edit { preferences -> preferences[key(model)] = id }
    }

    override suspend fun clear(model: GemmaModel) {
        dataStore.edit { preferences -> preferences.remove(key(model)) }
    }

    private fun key(model: GemmaModel) = longPreferencesKey("download_id_${model.name}")
}
