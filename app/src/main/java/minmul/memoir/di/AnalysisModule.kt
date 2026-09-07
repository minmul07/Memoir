package minmul.memoir.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import minmul.memoir.background.analysis.AnalysisRunner
import minmul.memoir.background.analysis.RandomFakeAnalysis
import minmul.memoir.core.ai.AnalysisLog
import minmul.memoir.core.ai.MultilingualOcrEngine
import minmul.memoir.core.ai.OcrModelManager
import minmul.memoir.core.model.OcrModel
import minmul.memoir.data.content.AnalysisRepository
import minmul.memoir.data.content.AnalysisRepositoryImpl
import minmul.memoir.data.content.ContentRepository
import minmul.memoir.data.preferences.OcrModelPreferencesStore
import minmul.memoir.data.preferences.UserPreferencesRepository
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

    @Provides @Singleton
    fun runner(
        repository: AnalysisRepository,
        content: ContentRepository,
        ocr: MultilingualOcrEngine
    ) =
        AnalysisRunner(repository, content, ocr, RandomFakeAnalysis(), AnalysisLog::write)
}
