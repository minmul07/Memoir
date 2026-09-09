package minmul.memoir.analysis

import kotlinx.coroutines.flow.first
import minmul.memoir.background.analysis.LocatedGemmaModel
import minmul.memoir.background.analysis.ReadyGemmaModelLocator
import minmul.memoir.core.model.GemmaModel
import minmul.memoir.core.model.preferredReadyGemmaModel
import minmul.memoir.data.model.GemmaModelStore
import minmul.memoir.data.preferences.GemmaModelPreferencesStore
import java.io.File

class StoreReadyGemmaModelLocator(
    private val store: GemmaModelStore,
    private val preferences: GemmaModelPreferencesStore,
) : ReadyGemmaModelLocator {
    override suspend fun locate(): LocatedGemmaModel? {
        val selected = preferences.selectedGemmaModel.first()
        val files = buildMap<GemmaModel, File> {
            for (model in GemmaModel.entries) {
                store.installedFile(model)?.let { put(model, it) }
            }
        }
        val chosen = preferredReadyGemmaModel(selected, files.keys) ?: return null
        val file = files[chosen] ?: return null
        return LocatedGemmaModel(chosen, file)
    }
}
