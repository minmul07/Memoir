package minmul.memoir.feature.main

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ItemDetailViewModelTest {
    @Test
    fun `deleteItem removes the item then notifies`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val analysis = FakeAnalysisRepository()
        val viewModel = ItemDetailViewModel(analysis)
        var deleted = false
        try {
            viewModel.deleteItem("item") { deleted = true }
            advanceUntilIdle()
            assertEquals(listOf("item"), analysis.deleted)
            assertTrue(deleted)
            assertEquals(false, viewModel.actionFailed.value)
            assertEquals(false, viewModel.busy.value)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `deleteItem failure exposes an error without notifying`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val analysis = FakeAnalysisRepository().apply { deleteFails = true }
        val viewModel = ItemDetailViewModel(analysis)
        var deleted = false
        try {
            viewModel.deleteItem("item") { deleted = true }
            advanceUntilIdle()
            assertTrue(analysis.deleted.isEmpty())
            assertEquals(false, deleted)
            assertTrue(viewModel.actionFailed.value)
            assertEquals(false, viewModel.busy.value)
        } finally {
            Dispatchers.resetMain()
        }
    }
}
