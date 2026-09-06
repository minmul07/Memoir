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
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class IntakeViewModelTest {
    @Test
    fun `incomplete onboarding opens onboarding without importing`() = runViewModelTest {
        val repository = FakeContentRepository()
        val viewModel = createViewModel(
            progress = OnboardingProgress.LANDING,
            repository = repository,
        )
        viewModel.start(listOf("content://images/1"))
        advanceUntilIdle()

        viewModel.uiState.test {
            assertEquals(IntakeUiState.OpenOnboarding, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals(emptyList<Pair<String, String>>(), repository.imported)
    }

    @Test
    fun `completed onboarding with images shows confirm and starts import`() = runViewModelTest {
        val uris = listOf("content://images/1", "content://images/2")
        val repository = FakeContentRepository()
        val viewModel = createViewModel(repository = repository)
        viewModel.start(uris)
        advanceUntilIdle()

        viewModel.uiState.test {
            assertEquals(
                IntakeUiState.Confirm(
                    drafts = uris.map { IntakeDraft(imageUri = it, failed = false) },
                    isSubmitting = false,
                ),
                awaitItem(),
            )
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals(uris, repository.imported.map { it.second })
    }

    @Test
    fun `add waits for import then enqueues successes and opens queue`() = runViewModelTest {
        val uri = "content://images/1"
        val repository = FakeContentRepository(holdUris = setOf(uri))
        val viewModel = createViewModel(repository = repository)
        viewModel.start(listOf(uri))
        advanceUntilIdle()

        viewModel.uiState.test {
            assertEquals(
                IntakeUiState.Confirm(
                    drafts = listOf(IntakeDraft(uri, failed = false)),
                    isSubmitting = false,
                ),
                awaitItem(),
            )

            viewModel.onAdd()
            advanceUntilIdle()
            assertEquals(
                IntakeUiState.Confirm(
                    drafts = listOf(IntakeDraft(uri, failed = false)),
                    isSubmitting = true,
                ),
                awaitItem(),
            )
            assertTrue(repository.enqueued.isEmpty())

            repository.release(uri)
            advanceUntilIdle()
            assertEquals(IntakeUiState.OpenQueue, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals(1, repository.enqueued.single().size)
        assertEquals(uri, repository.imported.single().second)
    }

    @Test
    fun `add enqueues only successful imports`() = runViewModelTest {
        val success = "content://images/1"
        val failure = "content://images/2"
        val repository = FakeContentRepository(failUris = setOf(failure))
        val viewModel = createViewModel(repository = repository)
        viewModel.start(listOf(success, failure))
        advanceUntilIdle()

        viewModel.onAdd()
        advanceUntilIdle()

        viewModel.uiState.test {
            assertEquals(IntakeUiState.OpenQueue, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals(listOf(success), repository.enqueued.single().map { imported ->
            repository.imported.first { it.first == imported.itemId }.second
        })
    }

    @Test
    fun `add stays on confirm when every import fails`() = runViewModelTest {
        val uris = listOf("content://images/1", "content://images/2")
        val repository = FakeContentRepository(failUris = uris.toSet())
        val viewModel = createViewModel(repository = repository)
        viewModel.start(uris)
        advanceUntilIdle()

        viewModel.onAdd()
        advanceUntilIdle()

        viewModel.uiState.test {
            assertEquals(
                IntakeUiState.Confirm(
                    drafts = uris.map { IntakeDraft(imageUri = it, failed = true) },
                    isSubmitting = false,
                ),
                awaitItem(),
            )
            cancelAndIgnoreRemainingEvents()
        }
        assertTrue(repository.enqueued.isEmpty())
    }

    @Test
    fun `cancel discards imported files and finishes`() = runViewModelTest {
        val repository = FakeContentRepository()
        val viewModel = createViewModel(repository = repository)
        viewModel.start(listOf("content://images/1"))
        advanceUntilIdle()

        viewModel.uiState.test {
            awaitItem()
            viewModel.onCancel()
            advanceUntilIdle()
            assertEquals(IntakeUiState.Finish, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals(
            repository.imported.map { it.first },
            repository.discarded.single(),
        )
    }

    @Test
    fun `empty images finish without confirm`() = runViewModelTest {
        val repository = FakeContentRepository()
        val viewModel = createViewModel(repository = repository)
        viewModel.start(emptyList())
        advanceUntilIdle()

        viewModel.uiState.test {
            assertEquals(IntakeUiState.Finish, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
        assertTrue(repository.imported.isEmpty())
    }

    private fun TestScope.createViewModel(
        progress: Int = OnboardingProgress.COMPLETED,
        repository: FakeContentRepository = FakeContentRepository(),
    ): IntakeViewModel = IntakeViewModel(
        onboardingProgressStore = FakeOnboardingProgressStore(progress),
        contentRepository = repository,
    )

    private fun runViewModelTest(testBody: suspend TestScope.() -> Unit) = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            testBody()
        } finally {
            Dispatchers.resetMain()
        }
    }
}
