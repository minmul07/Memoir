package minmul.memoir.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import minmul.memoir.analysis.StoreReadyGemmaModelLocator
import minmul.memoir.background.analysis.AnalysisRunner
import minmul.memoir.background.analysis.ReadyGemmaModelLocator
import minmul.memoir.core.ai.AnalysisLog
import minmul.memoir.core.ai.GemmaLlmEngine
import minmul.memoir.core.ai.LlmEngine
import minmul.memoir.core.ai.MultilingualOcrEngine
import minmul.memoir.core.ai.OcrModelManager
import minmul.memoir.core.model.OcrModel
import minmul.memoir.data.content.AnalysisRepository
import minmul.memoir.data.content.AnalysisRepositoryImpl
import minmul.memoir.data.content.ContentRepository
import minmul.memoir.data.model.GemmaModelStore
import minmul.memoir.data.preferences.GemmaModelPreferencesStore
import minmul.memoir.data.preferences.OcrModelPreferencesStore
import minmul.memoir.data.preferences.UserPreferencesRepository
import java.io.File
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AnalysisModule {
    @Provides @Singleton
    fun repository(impl: AnalysisRepositoryImpl): AnalysisRepository = impl

    @Provides @Singleton
    fun ocr(@ApplicationContext context: Context, preferences: OcrModelPreferencesStore) =
        MultilingualOcrEngine(context) { OcrModel.entries.toSet() - preferences.disabledOcrModels.first() }

    @Provides
    @Singleton
    fun ocrPreferences(repository: UserPreferencesRepository): OcrModelPreferencesStore = repository

    @Provides
    @Singleton
    fun models(ocr: MultilingualOcrEngine): OcrModelManager = ocr.modelManager

    @Provides
    @Singleton
    fun llmEngine(@ApplicationContext context: Context): GemmaLlmEngine = GemmaLlmEngine(context)

    @Provides
    @Singleton
    fun llm(engine: GemmaLlmEngine): LlmEngine = engine

    @Provides
    @Singleton
    fun gemmaLocator(
        store: GemmaModelStore,
        preferences: GemmaModelPreferencesStore,
    ): ReadyGemmaModelLocator = StoreReadyGemmaModelLocator(store, preferences)

    @Provides @Singleton
    fun runner(
        repository: AnalysisRepository,
        content: ContentRepository,
        ocr: MultilingualOcrEngine,
        llm: LlmEngine,
        models: ReadyGemmaModelLocator,
        @ApplicationContext context: Context,
    ) = AnalysisRunner(
        repository,
        content,
        ocr,
        llm,
        models,
        { relative -> File(context.filesDir, relative) },
        AnalysisLog::write,
    )
}
