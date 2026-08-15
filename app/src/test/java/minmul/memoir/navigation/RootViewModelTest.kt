package minmul.memoir.navigation

import app.cash.turbine.test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import minmul.memoir.data.preferences.OnboardingProgress
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RootViewModelTest {
    @Test
    fun `normalizes incomplete progress to landing`() = runViewModelTest {
        val viewModel = RootViewModel(FakeOnboardingProgressStore(OnboardingProgress.PERMISSION))
        advanceUntilIdle()

        viewModel.onboardingProgress.test {
            assertEquals(OnboardingProgress.LANDING, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `keeps model setup progress for resume`() = runViewModelTest {
        val viewModel = RootViewModel(FakeOnboardingProgressStore(OnboardingProgress.MODEL_SETUP))
        advanceUntilIdle()

        viewModel.onboardingProgress.test {
            assertEquals(OnboardingProgress.MODEL_SETUP, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `keeps completed progress`() = runViewModelTest {
        val viewModel = RootViewModel(FakeOnboardingProgressStore(OnboardingProgress.COMPLETED))
        advanceUntilIdle()

        viewModel.onboardingProgress.test {
            assertEquals(OnboardingProgress.COMPLETED, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `completeOnboarding writes completed progress`() = runViewModelTest {
        val viewModel = RootViewModel(FakeOnboardingProgressStore())
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
        val viewModel = RootViewModel(FakeOnboardingProgressStore(OnboardingProgress.COMPLETED))
        advanceUntilIdle()

        viewModel.onboardingProgress.test {
            awaitItem()
            viewModel.resetOnboarding()
            advanceUntilIdle()
            assertEquals(OnboardingProgress.LANDING, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
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
