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
import minmul.memoir.core.ai.OcrModelManager
import minmul.memoir.core.model.OcrModel
import minmul.memoir.core.model.OcrModelState
import minmul.memoir.core.model.OcrModelStatus
import minmul.memoir.data.preferences.OcrModelPreferencesStore
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OcrModelViewModelTest {
    @Test
    fun `saved choices are shown and toggling updates the shared preferences`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val preferences =
            FakePreferences().apply { disabledOcrModels.value = setOf(OcrModel.Japanese) }
        val viewModel = OcrModelViewModel(FakeModels(), preferences)
        try {
            advanceUntilIdle()
            assertFalse(viewModel.state.value.models.first { it.model == OcrModel.Japanese }.enabled)
            viewModel.setEnabled(OcrModel.Korean, false)
            advanceUntilIdle()
            assertEquals(
                setOf(OcrModel.Korean, OcrModel.Japanese),
                preferences.disabledOcrModels.value
            )
            assertTrue(viewModel.state.value.savingModels.isEmpty())
        } finally {
            viewModel.viewModelScope.cancel()
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `write failure preserves the checkbox and exposes an error`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val preferences = FakePreferences().apply { failWrites = true }
        val viewModel = OcrModelViewModel(FakeModels(), preferences)
        try {
            advanceUntilIdle()
            viewModel.setEnabled(OcrModel.Korean, false)
            advanceUntilIdle()
            assertTrue(viewModel.state.value.models.first { it.model == OcrModel.Korean }.enabled)
            assertTrue(viewModel.state.value.preferencesFailed)
            assertTrue(viewModel.state.value.savingModels.isEmpty())
        } finally {
            viewModel.viewModelScope.cancel()
            Dispatchers.resetMain()
        }
    }

    private class FakeModels : OcrModelManager {
        override val models =
            MutableStateFlow(OcrModel.entries.map { OcrModelState(it, OcrModelStatus.Ready) })

        override suspend fun refresh() = models.value
        override suspend fun install(model: OcrModel) = Unit
    }

    private class FakePreferences : OcrModelPreferencesStore {
        override val disabledOcrModels = MutableStateFlow<Set<OcrModel>>(emptySet())
        var failWrites = false
        override suspend fun setOcrModelEnabled(model: OcrModel, enabled: Boolean) {
            check(!failWrites) { "write_failed" }
            disabledOcrModels.value =
                if (enabled) disabledOcrModels.value - model else disabledOcrModels.value + model
        }
    }
}
