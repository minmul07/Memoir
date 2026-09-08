package minmul.memoir.data.preferences

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import minmul.memoir.core.model.GemmaModel
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
}
