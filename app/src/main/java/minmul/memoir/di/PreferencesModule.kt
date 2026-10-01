package minmul.memoir.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import minmul.memoir.data.preferences.AnalysisQueueModeStore
import minmul.memoir.data.preferences.InformationCollectionPreferencesStore
import minmul.memoir.data.preferences.OnboardingProgressStore
import minmul.memoir.data.preferences.UserPreferencesRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class PreferencesModule {
    @Binds
    @Singleton
    abstract fun bindInformationCollectionPreferencesStore(
        impl: UserPreferencesRepository,
    ): InformationCollectionPreferencesStore

    @Binds
    @Singleton
    abstract fun bindOnboardingProgressStore(
        impl: UserPreferencesRepository,
    ): OnboardingProgressStore

    @Binds
    @Singleton
    abstract fun bindAnalysisQueueModeStore(
        impl: UserPreferencesRepository,
    ): AnalysisQueueModeStore
}
