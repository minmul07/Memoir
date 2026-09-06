package minmul.memoir.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import minmul.memoir.core.storage.MemoirDatabase

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideMemoirDatabase(@ApplicationContext context: Context): MemoirDatabase =
        MemoirDatabase.create(context)
}
