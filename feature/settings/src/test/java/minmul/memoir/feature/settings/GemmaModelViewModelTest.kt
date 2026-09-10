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
import minmul.memoir.core.model.GemmaInferenceSettings
import minmul.memoir.core.model.GemmaModel
import minmul.memoir.core.model.GemmaModelState
import minmul.memoir.core.model.GemmaModelStatus
import minmul.memoir.data.model.GemmaModelStore
import minmul.memoir.data.preferences.GemmaModelPreferencesStore
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class GemmaModelViewModelTest {
    @Test
    fun `first ready model is selected when nothing is stored`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val preferences = FakePreferences()
        val viewModel = GemmaModelViewModel(FakeStore(ready = setOf(GemmaModel.E2B)), preferences)
        try {
            advanceUntilIdle()
            assertEquals(GemmaModel.E2B, preferences.selectedGemmaModel.value)
            assertTrue(viewModel.state.value.models.first { it.model == GemmaModel.E2B }.selected)
        } finally {
            viewModel.viewModelScope.cancel()
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `saved choice is shown and selecting updates the shared preferences`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val preferences = FakePreferences().apply { selectedGemmaModel.value = GemmaModel.E4B }
        val viewModel = GemmaModelViewModel(
            FakeStore(ready = setOf(GemmaModel.E4B, GemmaModel.E2B)),
            preferences,
        )
        try {
            advanceUntilIdle()
            assertTrue(viewModel.state.value.models.first { it.model == GemmaModel.E4B }.selected)
            viewModel.select(GemmaModel.E2B)
            advanceUntilIdle()
            assertEquals(GemmaModel.E2B, preferences.selectedGemmaModel.value)
            assertTrue(viewModel.state.value.savingModels.isEmpty())
        } finally {
            viewModel.viewModelScope.cancel()
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `saved choice is kept while its lookup is still checking`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val store = FakeStore(
            statuses = mapOf(
                GemmaModel.E4B to GemmaModelStatus.Ready,
                GemmaModel.E2B to GemmaModelStatus.Checking,
            ),
        )
        val preferences = FakePreferences().apply { selectedGemmaModel.value = GemmaModel.E2B }
        val viewModel = GemmaModelViewModel(store, preferences)
        try {
            advanceUntilIdle()
            assertEquals(GemmaModel.E2B, preferences.selectedGemmaModel.value)
            store.setStatus(GemmaModel.E2B, GemmaModelStatus.Ready)
            advanceUntilIdle()
            assertEquals(GemmaModel.E2B, preferences.selectedGemmaModel.value)
            assertTrue(viewModel.state.value.models.first { it.model == GemmaModel.E2B }.selected)
        } finally {
            viewModel.viewModelScope.cancel()
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `unavailable saved choice is replaced by the first ready model`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val store = FakeStore(
            statuses = mapOf(
                GemmaModel.E4B to GemmaModelStatus.Ready,
                GemmaModel.E2B to GemmaModelStatus.Missing,
            ),
        )
        val preferences = FakePreferences().apply { selectedGemmaModel.value = GemmaModel.E2B }
        val viewModel = GemmaModelViewModel(store, preferences)
        try {
            advanceUntilIdle()
            assertEquals(GemmaModel.E4B, preferences.selectedGemmaModel.value)
            assertTrue(viewModel.state.value.models.first { it.model == GemmaModel.E4B }.selected)
        } finally {
            viewModel.viewModelScope.cancel()
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `write failure preserves the radio and exposes an error`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val preferences = FakePreferences().apply {
            selectedGemmaModel.value = GemmaModel.E4B
            failWrites = true
        }
        val viewModel = GemmaModelViewModel(
            FakeStore(ready = setOf(GemmaModel.E4B, GemmaModel.E2B)),
            preferences,
        )
        try {
            advanceUntilIdle()
            viewModel.select(GemmaModel.E2B)
            advanceUntilIdle()
            assertEquals(GemmaModel.E4B, preferences.selectedGemmaModel.value)
            assertTrue(viewModel.state.value.models.first { it.model == GemmaModel.E4B }.selected)
            assertTrue(viewModel.state.value.preferencesFailed)
            assertTrue(viewModel.state.value.savingModels.isEmpty())
        } finally {
            viewModel.viewModelScope.cancel()
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `selected model is not deleted`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val store = FakeStore(ready = setOf(GemmaModel.E4B, GemmaModel.E2B))
        val preferences = FakePreferences().apply { selectedGemmaModel.value = GemmaModel.E4B }
        val viewModel = GemmaModelViewModel(store, preferences)
        try {
            advanceUntilIdle()
            viewModel.delete(GemmaModel.E4B)
            advanceUntilIdle()
            assertTrue(store.deleted.isEmpty())
            viewModel.delete(GemmaModel.E2B)
            advanceUntilIdle()
            assertEquals(listOf(GemmaModel.E2B), store.deleted)
        } finally {
            viewModel.viewModelScope.cancel()
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `install and cancel are forwarded`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val store = FakeStore()
        val viewModel = GemmaModelViewModel(store, FakePreferences())
        try {
            viewModel.install(GemmaModel.E4B)
            viewModel.cancel(GemmaModel.E4B)
            advanceUntilIdle()
            assertEquals(listOf(GemmaModel.E4B), store.installed)
            assertEquals(listOf(GemmaModel.E4B), store.cancelled)
        } finally {
            viewModel.viewModelScope.cancel()
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `inference settings load and setters update the shared preferences`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val preferences = FakePreferences().apply {
            inferenceSettings.value = GemmaInferenceSettings(
                maxOutputToken = 2048,
                topK = 16,
                thinkingEnabled = true,
                topP = 0.8,
                temperature = 0.5,
                speculativeDecodingEnabled = false,
            )
        }
        val viewModel = GemmaModelViewModel(FakeStore(), preferences)
        try {
            advanceUntilIdle()
            assertEquals(
                GemmaInferenceSettings(2048, 16, true, 0.8, 0.5, false),
                viewModel.state.value.inference,
            )
            viewModel.setMaxOutputToken(512)
            advanceUntilIdle()
            viewModel.setTopK(8)
            advanceUntilIdle()
            viewModel.setThinkingEnabled(false)
            advanceUntilIdle()
            viewModel.setTopP(0.7)
            advanceUntilIdle()
            viewModel.setTemperature(1.2)
            advanceUntilIdle()
            viewModel.setSpeculativeDecodingEnabled(true)
            advanceUntilIdle()
            assertEquals(
                GemmaInferenceSettings(512, 8, false, 0.7, 1.2, true),
                preferences.inferenceSettings.value,
            )
            assertEquals(
                GemmaInferenceSettings(512, 8, false, 0.7, 1.2, true),
                viewModel.state.value.inference,
            )
        } finally {
            viewModel.viewModelScope.cancel()
            Dispatchers.resetMain()
        }
    }

    private class FakeStore(
        ready: Set<GemmaModel> = emptySet(),
        statuses: Map<GemmaModel, GemmaModelStatus> = emptyMap(),
    ) : GemmaModelStore {
        override val models = MutableStateFlow(
            GemmaModel.entries.map { model ->
                GemmaModelState(
                    model,
                    statuses[model]
                        ?: if (model in ready) GemmaModelStatus.Ready else GemmaModelStatus.Missing,
                )
            },
        )

        fun setStatus(model: GemmaModel, status: GemmaModelStatus) {
            models.value = models.value.map {
                if (it.model == model) it.copy(status = status) else it
            }
        }

        val installed = mutableListOf<GemmaModel>()
        val cancelled = mutableListOf<GemmaModel>()
        val deleted = mutableListOf<GemmaModel>()

        override suspend fun refresh() = models.value
        override suspend fun install(model: GemmaModel) {
            installed += model
        }

        override suspend fun cancel(model: GemmaModel) {
            cancelled += model
        }

        override suspend fun delete(model: GemmaModel) {
            deleted += model
            models.value = models.value.map {
                if (it.model == model) it.copy(status = GemmaModelStatus.Missing) else it
            }
        }

        override fun installedFile(model: GemmaModel): File? = null
    }

    private class FakePreferences : GemmaModelPreferencesStore {
        override val selectedGemmaModel = MutableStateFlow<GemmaModel?>(null)
        override val inferenceSettings = MutableStateFlow(GemmaInferenceSettings())
        var failWrites = false
        override suspend fun setSelectedGemmaModel(model: GemmaModel) {
            check(!failWrites) { "write_failed" }
            selectedGemmaModel.value = model
        }

        override suspend fun setMaxOutputToken(value: Int) {
            check(!failWrites) { "write_failed" }
            inferenceSettings.value = inferenceSettings.value.copy(
                maxOutputToken = GemmaInferenceSettings.clamp(maxOutputToken = value).maxOutputToken,
            )
        }

        override suspend fun setTopK(value: Int) {
            check(!failWrites) { "write_failed" }
            inferenceSettings.value = inferenceSettings.value.copy(
                topK = GemmaInferenceSettings.clamp(topK = value).topK,
            )
        }

        override suspend fun setThinkingEnabled(enabled: Boolean) {
            check(!failWrites) { "write_failed" }
            inferenceSettings.value = inferenceSettings.value.copy(thinkingEnabled = enabled)
        }

        override suspend fun setTopP(value: Double) {
            check(!failWrites) { "write_failed" }
            inferenceSettings.value = inferenceSettings.value.copy(
                topP = GemmaInferenceSettings.clamp(topP = value).topP,
            )
        }

        override suspend fun setTemperature(value: Double) {
            check(!failWrites) { "write_failed" }
            inferenceSettings.value = inferenceSettings.value.copy(
                temperature = GemmaInferenceSettings.clamp(temperature = value).temperature,
            )
        }

        override suspend fun setSpeculativeDecodingEnabled(enabled: Boolean) {
            check(!failWrites) { "write_failed" }
            inferenceSettings.value = inferenceSettings.value.copy(speculativeDecodingEnabled = enabled)
        }
    }
}
