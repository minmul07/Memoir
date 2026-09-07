package minmul.memoir.navigation

import androidx.lifecycle.ViewModelStore
import app.cash.turbine.test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import minmul.memoir.core.model.JobStatus
import minmul.memoir.core.model.QueueItem
import minmul.memoir.data.content.ContentRepository
import minmul.memoir.intake.FakeContentRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WorkQueueViewModelTest {
    @Test
    fun `queue updates when saved items change`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val store = ViewModelStore()
        try {
            val queue = MutableStateFlow(emptyList<QueueItem>())
            val repository = object : ContentRepository by FakeContentRepository() {
                override fun observeQueue() = queue
            }
            val viewModel = WorkQueueViewModel(repository)
            store.put("queue", viewModel)
            viewModel.uiState.test {
                assertTrue(awaitItem().isLoading)
                assertEquals(WorkQueueUiState(isLoading = false), awaitItem())
                val item = QueueItem("job", "item", "items/item/original", JobStatus.Queued)
                queue.value = listOf(item)
                assertEquals(WorkQueueUiState(listOf(item), isLoading = false), awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        } finally {
            store.clear()
            advanceUntilIdle()
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `query failure produces an error state`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val store = ViewModelStore()
        try {
            val repository = object : ContentRepository by FakeContentRepository() {
                override fun observeQueue() = flow<List<QueueItem>> { error("database unavailable") }
            }
            val viewModel = WorkQueueViewModel(repository)
            store.put("queue", viewModel)
            viewModel.uiState.test {
                assertTrue(awaitItem().isLoading)
                assertEquals(WorkQueueUiState(isLoading = false, failed = true), awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        } finally {
            store.clear()
            advanceUntilIdle()
            Dispatchers.resetMain()
        }
    }
}
