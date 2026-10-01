package minmul.memoir.navigation

import app.cash.turbine.test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import minmul.memoir.FakeInformationCollectionPreferencesStore
import minmul.memoir.data.preferences.InformationCollectionPreferencesStore
import minmul.memoir.data.preferences.OnboardingProgress
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class RootViewModelTest {
    @Test
    fun `normalizes incomplete progress to landing`() = runViewModelTest {
        val viewModel = RootViewModel(
            FakeOnboardingProgressStore(OnboardingProgress.PERMISSION),
            FakeInformationCollectionPreferencesStore()
        )
        advanceUntilIdle()

        viewModel.onboardingProgress.test {
            assertEquals(OnboardingProgress.LANDING, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `keeps model setup progress for resume`() = runViewModelTest {
        val viewModel = RootViewModel(
            FakeOnboardingProgressStore(OnboardingProgress.MODEL_SETUP),
            FakeInformationCollectionPreferencesStore()
        )
        advanceUntilIdle()

        viewModel.onboardingProgress.test {
            assertEquals(OnboardingProgress.MODEL_SETUP, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `keeps completed progress`() = runViewModelTest {
        val viewModel = RootViewModel(
            FakeOnboardingProgressStore(OnboardingProgress.COMPLETED),
            FakeInformationCollectionPreferencesStore()
        )
        advanceUntilIdle()

        viewModel.onboardingProgress.test {
            assertEquals(OnboardingProgress.COMPLETED, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `completeOnboarding writes completed progress`() = runViewModelTest {
        val viewModel = RootViewModel(
            FakeOnboardingProgressStore(),
            FakeInformationCollectionPreferencesStore()
        )
        advanceUntilIdle()

        viewModel.onboardingProgress.test {
            awaitItem()
            viewModel.completeOnboarding()
            advanceUntilIdle()
            assertEquals(OnboardingProgress.COMPLETED, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `resetOnboarding writes landing progress`() = runViewModelTest {
        val viewModel = RootViewModel(
            FakeOnboardingProgressStore(OnboardingProgress.COMPLETED),
            FakeInformationCollectionPreferencesStore()
        )
        advanceUntilIdle()

        viewModel.onboardingProgress.test {
            awaitItem()
            viewModel.resetOnboarding()
            advanceUntilIdle()
            assertEquals(OnboardingProgress.LANDING, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `declining crash reports disables collection before advancing`() = runViewModelTest {
        val preferences = FakeInformationCollectionPreferencesStore()
        val progress = object : minmul.memoir.data.preferences.OnboardingProgressStore {
            override val onboardingProgress =
                kotlinx.coroutines.flow.MutableStateFlow(OnboardingProgress.CRASHLYTICS)

            override suspend fun normalizeOnboardingProgress() = Unit
            override suspend fun setOnboardingProgress(progress: Int) {
                assertEquals(false, preferences.informationCollectionEnabled.first())
                onboardingProgress.value = progress
            }
        }
        val viewModel = RootViewModel(progress, preferences)
        advanceUntilIdle()

        assertEquals(true, viewModel.declineCrashlytics())
        advanceUntilIdle()

        assertEquals(false, preferences.informationCollectionEnabled.first())
        assertEquals(OnboardingProgress.MODEL_SETUP, viewModel.onboardingProgress.value)
    }

    @Test
    fun `failed collection save keeps crash reports step for retry`() = runViewModelTest {
        val preferences = object : InformationCollectionPreferencesStore {
            override val informationCollectionEnabled = kotlinx.coroutines.flow.flowOf(true)
            override suspend fun setInformationCollectionEnabled(enabled: Boolean) {
                throw IOException("Save failed")
            }
        }
        val progress = object : minmul.memoir.data.preferences.OnboardingProgressStore {
            override val onboardingProgress =
                kotlinx.coroutines.flow.MutableStateFlow(OnboardingProgress.CRASHLYTICS)

            override suspend fun normalizeOnboardingProgress() = Unit
            override suspend fun setOnboardingProgress(progress: Int) {
                onboardingProgress.value = progress
            }
        }
        val viewModel = RootViewModel(progress, preferences)
        advanceUntilIdle()

        assertEquals(false, viewModel.declineCrashlytics())
        advanceUntilIdle()

        assertEquals(true, preferences.informationCollectionEnabled.first())
        assertEquals(OnboardingProgress.CRASHLYTICS, viewModel.onboardingProgress.value)
    }

    private fun runViewModelTest(testBody: suspend TestScope.() -> Unit) = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            testBody()
        } finally {
            Dispatchers.resetMain()
        }
    }
}
