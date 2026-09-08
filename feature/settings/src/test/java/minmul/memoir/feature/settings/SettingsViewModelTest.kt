package minmul.memoir.feature.settings

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import minmul.memoir.core.model.AnalysisQueueMode
import minmul.memoir.data.preferences.AnalysisQueueModeStore
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
    @Test
    fun `loaded preference is shown and selecting updates the shared store`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val preferences = FakePreferences().apply {
            analysisQueueMode.value = AnalysisQueueMode.Immediate
        }
        val viewModel = SettingsViewModel(preferences)
        try {
            advanceUntilIdle()
            assertEquals(AnalysisQueueMode.Immediate, viewModel.state.value.analysisQueueMode)
            assertTrue(viewModel.state.value.preferencesLoaded)

            viewModel.setAnalysisQueueMode(AnalysisQueueMode.Manual)
            advanceUntilIdle()

            assertEquals(AnalysisQueueMode.Manual, preferences.analysisQueueMode.value)
            assertEquals(AnalysisQueueMode.Manual, viewModel.state.value.analysisQueueMode)
            assertFalse(viewModel.state.value.saving)
        } finally {
            viewModel.viewModelScope.cancel()
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `write failure preserves the mode and exposes an error`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val preferences = FakePreferences().apply { failWrites = true }
        val viewModel = SettingsViewModel(preferences)
        try {
            advanceUntilIdle()
            viewModel.setAnalysisQueueMode(AnalysisQueueMode.Scheduled)
            advanceUntilIdle()
            assertEquals(AnalysisQueueMode.Immediate, preferences.analysisQueueMode.value)
            assertEquals(AnalysisQueueMode.Immediate, viewModel.state.value.analysisQueueMode)
            assertTrue(viewModel.state.value.preferencesFailed)
            assertFalse(viewModel.state.value.saving)
        } finally {
            viewModel.viewModelScope.cancel()
            Dispatchers.resetMain()
        }
    }

    private class FakePreferences : AnalysisQueueModeStore {
        override val analysisQueueMode = MutableStateFlow(AnalysisQueueMode.Immediate)
        var failWrites = false
        override suspend fun setAnalysisQueueMode(mode: AnalysisQueueMode) {
            check(!failWrites) { "write_failed" }
            analysisQueueMode.value = mode
        }
    }
}
