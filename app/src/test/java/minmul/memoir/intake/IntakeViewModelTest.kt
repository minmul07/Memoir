package minmul.memoir.intake

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
import minmul.memoir.navigation.FakeOnboardingProgressStore
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class IntakeViewModelTest {
    @Test
    fun `incomplete onboarding opens onboarding`() = runViewModelTest {
        val viewModel = IntakeViewModel(FakeOnboardingProgressStore(OnboardingProgress.LANDING))
        viewModel.start(listOf("content://images/1"))
        advanceUntilIdle()

        viewModel.uiState.test {
            assertEquals(IntakeUiState.OpenOnboarding, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `completed onboarding with images shows confirm`() = runViewModelTest {
        val uris = listOf("content://images/1", "content://images/2")
        val viewModel = IntakeViewModel(FakeOnboardingProgressStore(OnboardingProgress.COMPLETED))
        viewModel.start(uris)
        advanceUntilIdle()

        viewModel.uiState.test {
            assertEquals(IntakeUiState.Confirm(uris), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `add from confirm opens queue`() = runViewModelTest {
        val viewModel = IntakeViewModel(FakeOnboardingProgressStore(OnboardingProgress.COMPLETED))
        viewModel.start(listOf("content://images/1"))
        advanceUntilIdle()

        viewModel.uiState.test {
            assertEquals(IntakeUiState.Confirm(listOf("content://images/1")), awaitItem())
            viewModel.onAdd()
            advanceUntilIdle()
            assertEquals(IntakeUiState.OpenQueue, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `cancel from confirm finishes`() = runViewModelTest {
        val viewModel = IntakeViewModel(FakeOnboardingProgressStore(OnboardingProgress.COMPLETED))
        viewModel.start(listOf("content://images/1"))
        advanceUntilIdle()

        viewModel.uiState.test {
            awaitItem()
            viewModel.onCancel()
            advanceUntilIdle()
            assertEquals(IntakeUiState.Finish, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `empty images finish without confirm`() = runViewModelTest {
        val viewModel = IntakeViewModel(FakeOnboardingProgressStore(OnboardingProgress.COMPLETED))
        viewModel.start(emptyList())
        advanceUntilIdle()

        viewModel.uiState.test {
            assertEquals(IntakeUiState.Finish, awaitItem())
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
