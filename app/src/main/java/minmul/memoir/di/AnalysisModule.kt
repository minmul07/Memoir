package minmul.memoir.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import minmul.memoir.background.analysis.AnalysisRunner
import minmul.memoir.background.analysis.RandomFakeAnalysis
import minmul.memoir.core.ai.AnalysisLog
import minmul.memoir.core.ai.MultilingualOcrEngine
import minmul.memoir.data.content.AnalysisRepository
import minmul.memoir.data.content.AnalysisRepositoryImpl
import minmul.memoir.data.content.ContentRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AnalysisModule {
    @Provides @Singleton
    fun repository(impl: AnalysisRepositoryImpl): AnalysisRepository = impl

    @Provides @Singleton
    fun ocr(@ApplicationContext context: Context) = MultilingualOcrEngine(context)

    @Provides @Singleton
    fun runner(
        repository: AnalysisRepository,
        content: ContentRepository,
        ocr: MultilingualOcrEngine
    ) =
        AnalysisRunner(repository, content, ocr, RandomFakeAnalysis(), AnalysisLog::write)
}
