package minmul.memoir.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import minmul.memoir.data.content.ContentRepository
import minmul.memoir.data.content.ContentRepositoryImpl
import minmul.memoir.data.content.ImportDispatcher

@Module
@InstallIn(SingletonComponent::class)
abstract class ContentModule {
    @Binds
    @Singleton
    abstract fun bindContentRepository(impl: ContentRepositoryImpl): ContentRepository

    companion object {
        @JvmStatic
        @Provides
        @Singleton
        @ImportDispatcher
        fun provideImportDispatcher(): CoroutineDispatcher =
            Dispatchers.Default.limitedParallelism(2)
    }
}
