package minmul.memoir.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import minmul.memoir.core.ai.AnalysisLog
import minmul.memoir.core.design.R
import minmul.memoir.core.model.GemmaModel
import minmul.memoir.data.model.DataStoreGemmaDownloadIdStore
import minmul.memoir.data.model.DefaultGemmaModelStore
import minmul.memoir.data.model.DownloadManagerGemmaDownloadEngine
import minmul.memoir.data.model.GemmaModelFiles
import minmul.memoir.data.model.GemmaModelStore
import minmul.memoir.data.model.gemmaDownloadsDataStore
import minmul.memoir.data.preferences.GemmaModelPreferencesStore
import minmul.memoir.data.preferences.UserPreferencesRepository
import java.io.File
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ModelModule {
    @Provides
    @Singleton
    fun gemmaModels(@ApplicationContext context: Context): GemmaModelStore {
        val modelsDir = File(
            requireNotNull(context.getExternalFilesDir(null)) { "external_files_unavailable" },
            "models",
        )
        return DefaultGemmaModelStore(
            engine = DownloadManagerGemmaDownloadEngine(context),
            files = GemmaModelFiles(modelsDir),
            ids = DataStoreGemmaDownloadIdStore(context.gemmaDownloadsDataStore),
            scope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
            title = { model ->
                context.getString(
                    when (model) {
                        GemmaModel.E4B -> R.string.model_gemma_4_e4b
                        GemmaModel.E2B -> R.string.model_gemma_4_e2b
                    },
                )
            },
            log = AnalysisLog::write,
        )
    }

    @Provides
    fun gemmaPreferences(repository: UserPreferencesRepository): GemmaModelPreferencesStore =
        repository
}
