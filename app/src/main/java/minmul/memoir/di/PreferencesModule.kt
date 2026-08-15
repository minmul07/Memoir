package minmul.memoir.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import minmul.memoir.data.preferences.OnboardingProgressStore
import minmul.memoir.data.preferences.UserPreferencesRepository

@Module
@InstallIn(SingletonComponent::class)
abstract class PreferencesModule {
    @Binds
    @Singleton
    abstract fun bindOnboardingProgressStore(
        impl: UserPreferencesRepository,
    ): OnboardingProgressStore
}
