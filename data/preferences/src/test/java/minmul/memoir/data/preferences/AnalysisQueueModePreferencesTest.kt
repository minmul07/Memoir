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
import minmul.memoir.core.model.AnalysisQueueMode
import minmul.memoir.core.storage.UserPreferencesKeys
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class AnalysisQueueModePreferencesTest {
    @TempDir
    lateinit var directory: File

    @Test
    fun `analysis queue mode survives reopening the preferences file`() = runTest {
        val file = File(directory, "mode.preferences_pb")
        val firstScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        try {
            val repository = UserPreferencesRepository(
                PreferenceDataStoreFactory.create(scope = firstScope) { file },
            )
            assertEquals(AnalysisQueueMode.Immediate, repository.analysisQueueMode.first())
            repository.setAnalysisQueueMode(AnalysisQueueMode.Manual)
        } finally {
            firstScope.coroutineContext[Job]!!.cancelAndJoin()
        }

        val secondScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        try {
            val repository = UserPreferencesRepository(
                PreferenceDataStoreFactory.create(scope = secondScope) { file },
            )
            assertEquals(AnalysisQueueMode.Manual, repository.analysisQueueMode.first())
        } finally {
            secondScope.coroutineContext[Job]!!.cancelAndJoin()
        }
    }

    @Test
    fun `unknown analysis queue mode defaults to immediate`() = runTest {
        val file = File(directory, "mode.preferences_pb")
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        try {
            val dataStore = PreferenceDataStoreFactory.create(scope = scope) { file }
            dataStore.edit { preferences ->
                preferences[UserPreferencesKeys.ANALYSIS_QUEUE_MODE] = "Nope"
            }
            val repository = UserPreferencesRepository(dataStore)
            assertEquals(AnalysisQueueMode.Immediate, repository.analysisQueueMode.first())
        } finally {
            scope.coroutineContext[Job]!!.cancelAndJoin()
        }
    }
}
