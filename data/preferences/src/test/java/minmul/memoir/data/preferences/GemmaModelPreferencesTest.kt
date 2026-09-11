package minmul.memoir.data.preferences

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import minmul.memoir.core.model.GemmaInferenceSettings
import minmul.memoir.core.model.GemmaModel
import minmul.memoir.core.storage.UserPreferencesKeys
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class GemmaModelPreferencesTest {
    @TempDir
    lateinit var directory: File

    @Test
    fun `selected gemma model survives reopening the preferences file`() = runTest {
        val file = File(directory, "selection.preferences_pb")
        val firstScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        try {
            val repository = UserPreferencesRepository(
                PreferenceDataStoreFactory.create(scope = firstScope) { file },
            )
            assertNull(repository.selectedGemmaModel.first())
            repository.setSelectedGemmaModel(GemmaModel.E2B)
        } finally {
            firstScope.coroutineContext[Job]!!.cancelAndJoin()
        }

        val secondScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        try {
            val repository = UserPreferencesRepository(
                PreferenceDataStoreFactory.create(scope = secondScope) { file },
            )
            assertEquals(GemmaModel.E2B, repository.selectedGemmaModel.first())
        } finally {
            secondScope.coroutineContext[Job]!!.cancelAndJoin()
        }
    }

    @Test
    fun `inference settings default then survive reopening the preferences file`() = runTest {
        val file = File(directory, "inference.preferences_pb")
        val firstScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        try {
            val dataStore = PreferenceDataStoreFactory.create(scope = firstScope) { file }
            val repository = UserPreferencesRepository(dataStore)
            assertEquals(GemmaInferenceSettings(), repository.inferenceSettings.first())
            dataStore.edit { preferences ->
                preferences[UserPreferencesKeys.GEMMA_MAX_OUTPUT_TOKEN] = 2048
                preferences[UserPreferencesKeys.GEMMA_TOP_K] = 16
                preferences[UserPreferencesKeys.GEMMA_THINKING_ENABLED] = true
                preferences[UserPreferencesKeys.GEMMA_TOP_P] = 0.8
                preferences[UserPreferencesKeys.GEMMA_TEMPERATURE] = 0.5
                preferences[UserPreferencesKeys.GEMMA_SPECULATIVE_DECODING] = false
            }
            assertEquals(
                GemmaInferenceSettings(
                    maxOutputToken = 2048,
                    topK = 16,
                    thinkingEnabled = true,
                    topP = 0.8,
                    temperature = 0.5,
                    speculativeDecodingEnabled = false,
                ),
                repository.inferenceSettings.first(),
            )
        } finally {
            firstScope.coroutineContext[Job]!!.cancelAndJoin()
        }

        val secondScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        try {
            val repository = UserPreferencesRepository(
                PreferenceDataStoreFactory.create(scope = secondScope) { file },
            )
            assertEquals(
                GemmaInferenceSettings(
                    maxOutputToken = 2048,
                    topK = 16,
                    thinkingEnabled = true,
                    topP = 0.8,
                    temperature = 0.5,
                    speculativeDecodingEnabled = false,
                ),
                repository.inferenceSettings.first(),
            )
        } finally {
            secondScope.coroutineContext[Job]!!.cancelAndJoin()
        }
    }

    @Test
    fun `out of range inference settings are clamped`() = runTest {
        val file = File(directory, "inference-range.preferences_pb")
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        try {
            val dataStore = PreferenceDataStoreFactory.create(scope = scope) { file }
            dataStore.edit { preferences ->
                preferences[UserPreferencesKeys.GEMMA_MAX_OUTPUT_TOKEN] = 50
                preferences[UserPreferencesKeys.GEMMA_TOP_K] = 400
                preferences[UserPreferencesKeys.GEMMA_TOP_P] = 4.0
                preferences[UserPreferencesKeys.GEMMA_TEMPERATURE] = -1.0
            }
            val repository = UserPreferencesRepository(dataStore)
            assertEquals(
                GemmaInferenceSettings(
                    maxOutputToken = 128,
                    topK = 128,
                    topP = 1.0,
                    temperature = 0.0
                ),
                repository.inferenceSettings.first(),
            )
        } finally {
            scope.coroutineContext[Job]!!.cancelAndJoin()
        }

        val writeScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        try {
            assertEquals(
                4096,
                applySetter(writeScope, "max") { it.setMaxOutputToken(99_999) }.maxOutputToken,
            )
            assertEquals(1, applySetter(writeScope, "k") { it.setTopK(0) }.topK)
            assertEquals(0.0, applySetter(writeScope, "p") { it.setTopP(-1.0) }.topP)
            assertEquals(2.0, applySetter(writeScope, "t") { it.setTemperature(9.0) }.temperature)
        } finally {
            writeScope.coroutineContext[Job]!!.cancelAndJoin()
        }
    }

    private suspend fun applySetter(
        scope: CoroutineScope,
        name: String,
        write: suspend (UserPreferencesRepository) -> Unit,
    ): GemmaInferenceSettings {
        val repository = UserPreferencesRepository(
            PreferenceDataStoreFactory.create(scope = scope) {
                File(directory, "inference-range-write-$name.preferences_pb")
            },
        )
        write(repository)
        return repository.inferenceSettings.first()
    }
}
