package minmul.memoir.feature.settings

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import minmul.memoir.core.model.AnalysisQueueMode
import minmul.memoir.data.preferences.AnalysisQueueModeStore
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsInformationCollectionTest {
    @Test
    fun `saved preference is displayed and toggling updates the shared store`() {
        val preferences = FakeInformationCollectionPreferencesStore(false)
        withViewModel(preferences) { viewModel ->
            advanceUntilIdle()
            assertEquals(false, viewModel.state.value.informationCollectionEnabled)

            viewModel.setInformationCollectionEnabled(true)
            advanceUntilIdle()

            assertTrue(preferences.savedEnabled.value)
            assertEquals(true, viewModel.state.value.informationCollectionEnabled)
            assertFalse(viewModel.state.value.informationCollectionSaving)

            preferences.setInformationCollectionEnabled(false)
            advanceUntilIdle()
            assertEquals(false, viewModel.state.value.informationCollectionEnabled)
        }
    }

    @Test
    fun `write failure preserves the saved preference and retry clears the error`() {
        val preferences = FakeInformationCollectionPreferencesStore().apply { failWrites = true }
        withViewModel(preferences) { viewModel ->
            advanceUntilIdle()
            viewModel.setInformationCollectionEnabled(false)
            advanceUntilIdle()

            assertTrue(preferences.savedEnabled.value)
            assertEquals(true, viewModel.state.value.informationCollectionEnabled)
            assertTrue(viewModel.state.value.informationCollectionFailed)
            assertFalse(viewModel.state.value.informationCollectionSaving)

            preferences.failWrites = false
            viewModel.setInformationCollectionEnabled(false)
            advanceUntilIdle()
            assertEquals(false, viewModel.state.value.informationCollectionEnabled)
            assertFalse(viewModel.state.value.informationCollectionFailed)
        }
    }

    @Test
    fun `read failure prevents writes without disabling analysis queue settings`() {
        val preferences = FakeInformationCollectionPreferencesStore().apply {
            informationCollectionEnabled = flow { throw IOException("Read failed") }
        }
        withViewModel(preferences) { viewModel ->
            advanceUntilIdle()

            assertNull(viewModel.state.value.informationCollectionEnabled)
            assertTrue(viewModel.state.value.informationCollectionFailed)
            assertTrue(viewModel.state.value.preferencesLoaded)
            assertFalse(viewModel.state.value.preferencesFailed)

            viewModel.setInformationCollectionEnabled(false)
            advanceUntilIdle()
            assertEquals(0, preferences.writeCount)
        }
    }

    @Test
    fun `toggle ignores changes before preferences load and while a write is pending`() {
        val gate = CompletableDeferred<Unit>()
        val preferences = FakeInformationCollectionPreferencesStore().apply { writeGate = gate }
        withViewModel(preferences) { viewModel ->
            viewModel.setInformationCollectionEnabled(false)
            assertEquals(0, preferences.writeCount)
            advanceUntilIdle()

            viewModel.setInformationCollectionEnabled(false)
            advanceUntilIdle()
            assertTrue(viewModel.state.value.informationCollectionSaving)
            assertEquals(true, viewModel.state.value.informationCollectionEnabled)

            viewModel.setInformationCollectionEnabled(false)
            advanceUntilIdle()
            assertEquals(1, preferences.writeCount)

            gate.complete(Unit)
            advanceUntilIdle()
            assertEquals(false, viewModel.state.value.informationCollectionEnabled)
            assertFalse(viewModel.state.value.informationCollectionSaving)
        }
    }

    private fun withViewModel(
        preferences: FakeInformationCollectionPreferencesStore,
        body: suspend TestScope.(SettingsViewModel) -> Unit,
    ) = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val analysisPreferences = object : AnalysisQueueModeStore {
            override val analysisQueueMode = MutableStateFlow(AnalysisQueueMode.Manual)
            override suspend fun setAnalysisQueueMode(mode: AnalysisQueueMode) {
                analysisQueueMode.value = mode
            }
        }
        val viewModel = SettingsViewModel(analysisPreferences, preferences)
        try {
            body(viewModel)
        } finally {
            viewModel.viewModelScope.cancel()
            Dispatchers.resetMain()
        }
    }
}
