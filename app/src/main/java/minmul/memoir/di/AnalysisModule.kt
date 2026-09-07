package minmul.memoir.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import minmul.memoir.core.ai.KoreanOcrEngine
import minmul.memoir.background.analysis.AnalysisRunner
import minmul.memoir.background.analysis.RandomFakeAnalysis
import minmul.memoir.data.content.*

@Module
@InstallIn(SingletonComponent::class)
object AnalysisModule {
    @Provides @Singleton
    fun repository(impl: AnalysisRepositoryImpl): AnalysisRepository = impl

    @Provides @Singleton
    fun ocr(@ApplicationContext context: Context) = KoreanOcrEngine(context)

    @Provides @Singleton
    fun runner(repository: AnalysisRepository, content: ContentRepository, ocr: KoreanOcrEngine) =
        AnalysisRunner(repository, content, ocr, RandomFakeAnalysis())
}
